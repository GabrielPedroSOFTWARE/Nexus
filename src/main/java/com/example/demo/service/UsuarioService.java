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

		validarSolicitacaoCadastro(novoUsuario, cpfCriptografado);
		
		novoUsuario.setCpf(cpfCriptografado);
		
        String senhaSegura = encriptador.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaSegura);
        
        novoUsuario.setDataHoraCadastro(formatadorDataHora.format(LocalDateTime.now()));
        
        usuarioRepository.save(novoUsuario);
    }
	
	
	// Valida a solicitação de cadastro
	private void validarSolicitacaoCadastro(Usuario novoUsuario, String cpfCriptografado) {
	    if (novoUsuario.isEhAdmin() && usuarioRepository.existsByEhAdminTrue()) {
	        throw new RuntimeException("Já existe um adm cadastrado");
	    }

	    boolean cadastroDiretoSuporteOuUsuario = 
	    		novoUsuario.getPerfil() == PerfilUsuario.SUPPORT_MANAGER
	            || "Ativo".equals(novoUsuario.getStatus());
	    
	    if (cadastroDiretoSuporteOuUsuario && !novoUsuario.isEhAdmin()) {
	        throw new RuntimeException("Não é permitido se cadastrar diretamente, "
	                + "faça primeiro uma solicitação de cadastro");
	    }

	    Usuario existente = (Usuario) usuarioRepository.findByCpf(cpfCriptografado);
	    if (existente != null) {
	        String mensagem = existente.getPerfil() == PerfilUsuario.PENDENT
	                ? "Solicitação já enviada, aguarde a aprovação"
	                : "Este CPF já está cadastrado";
	        throw new RuntimeException(mensagem);
	    }
	}
    
	
	// Verifica se há algum usuário pendente
	public List<Usuario> verificarPendentes() {
	    List<Usuario> pendentes = 
	    		usuarioRepository.findByStatusOrderByDataHoraCadastroDesc("Aguardando análise");
	    return preencherCpfMascarado(pendentes);
	}
    
    
    // Atualiza o status do usuário
	public void atualizarStatusUsuario(Map<String, String> dados) {
	    PerfilUsuario perfilDefinido = PerfilUsuario.valueOf(dados.get("perfil"));

	    Usuario usuario = (Usuario) usuarioRepository.findByCpf(dados.get("cpf"));
	    if (usuario == null) {
	        throw new RuntimeException("Usuário não encontrado");
	    }

	    usuario.setStatus("Ativo");
	    usuario.setPerfil(perfilDefinido);

	    usuarioRepository.save(usuario);

	    try {
	        emailService.enviarEmailSolicitacaoAceita(
	                usuario.getEmail(),
	                usuario.getNome(),
	                perfilDefinido
	        );
	    } catch (Exception e) {
	        System.err.println("Usuário aprovado, mas falhou o envio do e-mail: " + e.getMessage());
	    }
	}
    
    
    // Nega a solicitação do usuário
	public void negarSolicitacaoUsuario(String cpf) {
		
		cpf = cpf.replace("\"", "");
	    Usuario usuario = (Usuario) usuarioRepository.findByCpf(cpf);

	    if (usuario == null) {
	        throw new RuntimeException("Usuário não encontrado");
	    }

	    if (!"Aguardando análise".equals(usuario.getStatus())) {
	        throw new RuntimeException("Usuário já está cadastrado");
	    }

	    String emailDestino = usuario.getEmail();
	    String nomeUsuario = usuario.getNome();

	    usuarioRepository.deleteById(cpf);

	    try {
	        emailService.enviarEmailSolicitacaoNegada(emailDestino, nomeUsuario);
	    } catch (Exception e) { e.printStackTrace(); }
	}
    
    
    // Atualiza múltiplos usuários de uma só vez
	public void salvarAlteracoesEmLote(List<AtualizacaoUsuarioDTO> alteracoes, Usuario usuarioLogado) {
	    for (AtualizacaoUsuarioDTO dto : alteracoes) {
	        dto.setCpf(dto.getCpf().replaceAll("\"", ""));
	        Usuario usuario = usuarioRepository.findById(dto.getCpf())
	                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + dto.getCpf()));

	        boolean alterado = houveAlteracao(usuario, dto);
	        validarAlteracaoUsuario(usuario, dto, usuarioLogado, alterado);
	        if (alterado) {
	            usuario.setVersaoToken(usuario.getVersaoToken() + 1);
	        }

	        salvarHistoricoAlteracoesUsuario(usuario, dto);
	        usuario.setPerfil(dto.getPerfil());
	        usuario.setEhAdmin(dto.isEhAdmin());
	        usuario.setStatus(dto.getStatus());
	        usuarioRepository.save(usuario);
	    }
	}
	
	
	// Verifica se houve alguma alteração no usuário
	private boolean houveAlteracao(Usuario usuario, AtualizacaoUsuarioDTO dto) {
	    return usuario.getPerfil() != dto.getPerfil()
	            || usuario.isEhAdmin() != dto.isEhAdmin()
	            || !usuario.getStatus().equals(dto.getStatus());
	}

	
	// Valida a alteração do usuário
	private void validarAlteracaoUsuario(Usuario usuario, AtualizacaoUsuarioDTO dto,
	        Usuario usuarioLogado, boolean alterado) {
	    if (dto.getPerfil() == PerfilUsuario.USER && dto.isEhAdmin()) {
	        throw new RuntimeException("Um Usuário não pode ser Admin");
	    }
	    if (usuarioLogado.getCpf().equals(usuario.getCpf())) {
	        throw new RuntimeException("Um ADM não pode alterar seu próprio status");
	    }
	    if (alterado && "Inativo".equals(usuario.getStatus())) {
	        throw new RuntimeException("Um Usuário Inativo não pode ser alterado");
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
        List<Usuario> usuarios = usuarioRepository.findByCpfNot(usuarioLogado.getCpf());
        return preencherCpfMascarado(usuarios);
    }
    
    
    // Recebe o CPF, gera os dois códigos (o do e-mail e o "bipe" da tela) e envia o link
    public String solicitarRecuperacaoSenha(String cpf) {
    	cpf = cpf.replaceAll("\\D", "");
        Usuario usuario = usuarioRepository.findById(criptografiaService.criptografar(cpf))
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
        usuario.setValidadeTokenRecuperacao(LocalDateTime.now().plusMinutes(30));
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
    
    private List<Usuario> preencherCpfMascarado(List<Usuario> usuarios) {
        for (Usuario usuario : usuarios) {
            usuario.setCpfDescriptografado(
                    criptografiaService.descriptografarEMascarar(usuario.getCpf())
            );
        }
        return usuarios;
    }
}
