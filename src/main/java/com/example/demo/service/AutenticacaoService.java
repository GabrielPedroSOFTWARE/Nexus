package com.example.demo.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.DadosLogin;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class AutenticacaoService implements UserDetailsService {

	@Autowired
	@Lazy
	private AuthenticationManager manager;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TokenService tokenService;
    
    
    // Verifica se o login está inativo ou necessita de aprovação
    public ResponseEntity<?> verificarStatusUsuario(String login) {
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(login);
        
        if (usuario == null) return null;
        
        return switch (usuario.getStatus()) {
            case "Aguardando análise" -> ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Conta aguardando aprovação.");
            
            case "Inativo" -> ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body("Conta inativa.");
            
            default -> null;
        };
    }
    
    
    // Efetua o login
    public ResponseEntity<?> efetuarLogin(DadosLogin dados) { 
        
        ResponseEntity<?> erroDeStatus = verificarStatusUsuario(dados.login());
        
        if (erroDeStatus != null) return erroDeStatus;
        
        Usuario logado = (Usuario) manager.authenticate(
                new UsernamePasswordAuthenticationToken(dados.login(), dados.senha())
            ).getPrincipal();
            return ResponseEntity.ok(Map.of(
                "token", tokenService.gerarToken(logado),
                "perfil", logado.getPerfil().name(),
                "admin", String.valueOf(logado.isEhAdmin())
            ));
    }
    
    
    // Verifica se o usuário cadastrado
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByLogin(username);
    }
}