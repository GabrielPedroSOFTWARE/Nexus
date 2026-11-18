package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

	
	// Realiza a solicitação de cadastro do usuário
	public void cadastrarUsuario(Usuario novoUsuario) {
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
    public void atualizarStatusUsuario(String id, PerfilUsuario perfilDefinido) {
        
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        
        Usuario usuario = usuarioOptional.get();
        
        usuario.setStatus("Ativo");
        usuario.setPerfil(perfilDefinido);
        
        usuarioRepository.save(usuario);
    }
    
    
    // Nega a solicitação do usuário
    public void negarSolicitacaoUsuario(String id) {
    	
    	Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if(!usuario.getStatus().equals("Aguardando análise")) {
        	throw new RuntimeException("Usuário já está cadastrado");
        }
        
    	usuarioRepository.deleteById(id);
    }
    
    
    // Atualiza múltiplos usuários de uma só vez
    public void salvarAlteracoesEmLote(List<AtualizacaoUsuarioDTO> alteracoes) {
        
        for (AtualizacaoUsuarioDTO dto : alteracoes) {
            
            Usuario usuario = usuarioRepository.findById(dto.getCpf())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + dto.getCpf()));
            
            if (usuario.getPerfil() != dto.getPerfil() || usuario.isEhAdmin() != dto.isEhAdmin() 
            		|| !usuario.getStatus().endsWith(dto.getStatus())) {
            	usuario.setVersaoToken(usuario.getVersaoToken() + 1);   
            }
            
            salvarHistoricoAlteracoesUsuario(usuario, dto);
            
            usuario.setPerfil(dto.getPerfil());
            usuario.setEhAdmin(dto.isEhAdmin());
            usuario.setStatus(dto.getStatus());
            
            usuarioRepository.save(usuario);
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
}
