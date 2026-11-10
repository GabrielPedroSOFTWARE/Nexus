package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class AutenticacaoService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
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

    // Verifica se o usuário cadastrado
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByLogin(username);
    }
}