package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.AtualizacaoUsuarioDTO;
import com.example.demo.model.HistoricoUsuario;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.HistoricoUsuarioRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService {
	
	@Autowired
	UsuarioRepository usuarioRepository;
	@Autowired
    private PasswordEncoder encriptador;
	@Autowired
	private HistoricoUsuarioRepository historicoUsuarioRepository;
	private DateTimeFormatter formatadorDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	@Autowired
    private EmailService emailService;
	@Autowired
	private CriptografiaCpfService criptografiaService;
	
	// Realiza a solicitação de cadastro do usuário
	public void cadastrarUsuario(Usuario novoUsuario) {
		
		String cpfCriptografado = criptografiaService.criptografar(novoUsuario.getCpf());
		novoUsuario.setCpf(cpfCriptografado);
		
        String senhaSegura = encriptador.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaSegura);
        
        novoUsuario.setDataHoraCadastro(LocalDateTime.now());
        
        usuarioRepository.save(novoUsuario);
    }
    
	
	// Verifica se há algum usuário pendente
    public List<Usuario> verificarPendentes() {
        return usuarioRepository.findByStatus("Aguardando análise");
    }
    
    
    // Atualiza o status do usuário
    public void atualizarStatusUsuario(Map<String, String> dados) {
        
    	String cpf = dados.get("cpf").replaceAll("\\D", "");
    	cpf = criptografiaService.criptografar(cpf);
    	
    	PerfilUsuario perfilDefinido = PerfilUsuario.valueOf(dados.get("perfil"));
    	
        Usuario usuario = (Usuario) usuarioRepository.findByCpf(cpf);
        
        usuario.setStatus("Ativo");
        usuario.setPerfil(perfilDefinido);
        
        usuarioRepository.save(usuario);
    }
    
    
    // Nega a solicitação do usuário
    public void negarSolicitacaoUsuario(String cpf) {
    	
    	cpf = criptografiaService.criptografar(cpf);
    	
    	Usuario usuario = (Usuario) usuarioRepository.findByCpf(cpf);
        
        if(!usuario.getStatus().equals("Aguardando análise")) {
        	throw new RuntimeException("Usuário já está cadastrado");
        }
        
    	usuarioRepository.deleteById(cpf);
    }
    
    
    // Atualiza múltiplos usuários de uma só vez
    public void salvarAlteracoesEmLote(List<AtualizacaoUsuarioDTO> alteracoes, Usuario usuarioLogado) {
        
        for (AtualizacaoUsuarioDTO dto : alteracoes) {
            dto.setCpf(dto.getCpf().replaceAll("\\D", ""));
            
            Usuario usuarioSelecionado = usuarioRepository.findById(dto.getCpf())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + dto.getCpf()));
            
            if(usuarioSelecionado.getPerfil().equals(PerfilUsuario.USER) && dto.getPerfil().equals(PerfilUsuario.USER) 
            		&& dto.isEhAdmin()) {
            	throw new RuntimeException("Um Usuário não pode ser Admin");
            }
            
            if(usuarioLogado.getCpf().equals(usuarioSelecionado.getCpf())) {
            	throw new RuntimeException("Um ADM não pode alterar seu próprio status");
            }
            
            if (usuarioSelecionado.getPerfil() != dto.getPerfil() || usuarioSelecionado.isEhAdmin() != dto.isEhAdmin() 
            		|| !usuarioSelecionado.getStatus().endsWith(dto.getStatus())) {
            	if(usuarioSelecionado.getStatus().equals("Inativo")) {
            		throw new RuntimeException("Um Usuário Inativo não pode ser alterado");
            	}
            	usuarioSelecionado.setVersaoToken(usuarioSelecionado.getVersaoToken() + 1);   
            }
            
            salvarHistoricoAlteracoesUsuario(usuarioSelecionado, dto);
            
            usuarioSelecionado.setPerfil(dto.getPerfil());
            usuarioSelecionado.setEhAdmin(dto.isEhAdmin());
            usuarioSelecionado.setStatus(dto.getStatus());
            
            usuarioRepository.save(usuarioSelecionado);
        }
    }
    
    
    // Salva em um histórico quais status foram alterados
    private void salvarHistoricoAlteracoesUsuario(Usuario usuario, AtualizacaoUsuarioDTO dto) {
    	
    	List<String> mudancas = new ArrayList<>();

        if (usuario.getPerfil() != dto.getPerfil()) 
            mudancas.add("Perfil: " + usuario.getPerfil() + " -> " + dto.getPerfil());
        
        if (usuario.isEhAdmin() != dto.isEhAdmin()) 
            mudancas.add("Admin: " + usuario.isEhAdmin() + " -> " + dto.isEhAdmin());
        
        if (!usuario.getStatus().equals(dto.getStatus())) 
            mudancas.add("Status: " + usuario.getStatus() + " -> " + dto.getStatus());

        if (mudancas.isEmpty()) return;

        HistoricoUsuario historico = new HistoricoUsuario();
        historico.setCpfUsuario(usuario.getCpf());
        
        historico.setDescricaoAlteracao(String.join("\n", mudancas)); 
        historico.setDataHoraAlteracao(LocalDateTime.now());
        
        historicoUsuarioRepository.save(historico);
    }
    
    
    // Retorna os usuários logados, menos o admin
    public List<Usuario> retornarUsuarios(Usuario usuarioLogado) {
    	return usuarioRepository.findByCpfNot(usuarioLogado.getCpf());
    }
    
    
    // Recebe o CPF, gera os dois códigos (o do e-mail e o "bipe" da tela) e envia o link
    public String solicitarRecuperacaoSenha(String cpf) {
    	String cpfCriptografado = criptografiaService.criptografar(cpf);
        Usuario usuario = usuarioRepository.findById(cpfCriptografado)
                .orElseThrow(() -> new RuntimeException("CPF não encontrado no sistema."));
        
        if(usuario.getStatus().equals("Inativo")) {
        	throw new RuntimeException("Essa conta não está ativa");
        }
        
        if(usuario.getStatus().equals("Aguardando análise")) {
        	throw new RuntimeException("Essa conta ainda não foi ativada");
        }
        String tokenEmail = UUID.randomUUID().toString();
        String codigoEspera = UUID.randomUUID().toString();

        usuario.setTokenRecuperacao(tokenEmail);
        usuario.setCodigoEspera(codigoEspera);
        usuario.setRecuperacaoConfirmada(false);
        usuario.setValidadeTokenRecuperacao(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        String link = "http://localhost:8080/confirmar-recuperacao?token=" + tokenEmail;
        emailService.enviarEmailRecuperacao(usuario.getEmail(), link);

        return codigoEspera;
    }

    // Quando o usuário clica no link do e-mail, este método muda o atributo da recuperação para "true"
    public void confirmarCliqueNoLink(String tokenEmail) {
        Usuario usuario = usuarioRepository.findByTokenRecuperacao(tokenEmail)
                .orElseThrow(() -> new RuntimeException("Link inválido."));

        if (usuario.getValidadeTokenRecuperacao() == null ||
            LocalDateTime.now().isAfter(usuario.getValidadeTokenRecuperacao())) {
            throw new RuntimeException("Link expirado.");
        }

        usuario.setRecuperacaoConfirmada(true);
        usuarioRepository.save(usuario);
    }

    // A tela em espera chama isso de tempos em tempos para saber se já pode destrancar
    public boolean verificarSeConfirmou(String codigoEspera) {
        return usuarioRepository.findByCodigoEspera(codigoEspera)
                .map(u -> u.isRecuperacaoConfirmada()
                        && u.getValidadeTokenRecuperacao() != null
                        && LocalDateTime.now().isBefore(u.getValidadeTokenRecuperacao()))
                .orElse(false);
    }

    // Depois que a tela destrancou, salva a nova senha criptografada e apaga os códigos
    public void redefinirSenha(String codigoEspera, String novaSenha) {
        Usuario usuario = usuarioRepository.findByCodigoEspera(codigoEspera)
                .orElseThrow(() -> new RuntimeException("Solicitação inválida."));

        if (!usuario.isRecuperacaoConfirmada() ||
            usuario.getValidadeTokenRecuperacao() == null ||
            LocalDateTime.now().isAfter(usuario.getValidadeTokenRecuperacao())) {
            throw new RuntimeException("Confirmação pendente ou expirada.");
        }

        usuario.setSenha(encriptador.encode(novaSenha));

        // Limpa tudo para o link e o código não poderem ser usados uma segunda vez
        usuario.setTokenRecuperacao(null);
        usuario.setCodigoEspera(null);
        usuario.setRecuperacaoConfirmada(false);
        usuario.setValidadeTokenRecuperacao(null);

        usuarioRepository.save(usuario);
    }
}
