package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    
    
    // Altera o status da conta do usuário
    public ResponseEntity<String> alterarStatusConta(String id) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
    	
    	if(usuario.getStatus().equals("Ativo")) usuario.setStatus("Inativo");
    		else usuario.setStatus("Ativo");
    	
    	usuarioRepository.save(usuario);
    	return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: "
    	    	+ usuario.getStatus());
    }
    
    
    // Altera a permissão de admin do usuário
    public ResponseEntity<String> alterarPermissaoAdmin(String id) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
        
        if(usuario.isEhAdmin()) usuario.setEhAdmin(false);
        	else usuario.setEhAdmin(true);
        
        usuarioRepository.save(usuario);
        
        String situacaoAdminUsuario = (usuario.isEhAdmin()) ? 
        		"é admin" : "não é mais admin";
        
        return ResponseEntity.ok("Situação de admin do usuário: " + situacaoAdminUsuario);
    }
    
    
    // Altera o perfil da conta do usuário
    public ResponseEntity<String> alterarPerfilConta(String id, PerfilUsuario perfil) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
        
        usuario.setPerfil(perfil);
        usuario.setVersaoToken(usuario.getVersaoToken() + 1);
        
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Perfil da conta alterado para " + perfil.toString());
    }
    
    
    // Retorna os usuários logados, menos o admin
    public List<Usuario> retornarUsuarios(Usuario usuarioLogado) {
    	return usuarioRepository.findByIdNot(usuarioLogado.getId());
    }
}
