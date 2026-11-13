package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.AtualizacaoUsuarioDTO;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioService {
	
	@Autowired
	UsuarioRepository usuarioRepository;
	@Autowired
    private PasswordEncoder encriptador;

	
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
            
            Usuario usuario = usuarioRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + dto.getId()));
            
            if (usuario.getPerfil() != dto.getPerfil() || usuario.isEhAdmin() != dto.isEhAdmin() 
            		|| !usuario.getStatus().endsWith(dto.getStatus())) {
            	usuario.setVersaoToken(usuario.getVersaoToken() + 1);   
            }
            
            usuario.setPerfil(dto.getPerfil());
            usuario.setEhAdmin(dto.isEhAdmin());
            usuario.setStatus(dto.getStatus());
            
            usuarioRepository.save(usuario);
        }
    }
    
    // Retorna os usuários logados, menos o admin
    public List<Usuario> retornarUsuarios(Usuario usuarioLogado) {
    	return usuarioRepository.findByIdNot(usuarioLogado.getId());
    }
}
