package com.example.demo.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {
	
    @Autowired
    private TokenService tokenService; 
    @Autowired
    private UsuarioRepository usuarioRepository; 

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = recuperarToken(request);
        
        if (token != null) verificarToken(token);
        
        filterChain.doFilter(request, response);
    }

    // Verifica o token
    private void verificarToken(String token) {
        String login = tokenService.validarToken(token);
        if (login == null) return;
        
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(login);
        if (usuario == null) return;
        
        Integer versaoNoToken = tokenService.extrairVersaoToken(token);
        if (versaoNoToken == null || !versaoNoToken.equals(usuario.getVersaoToken())) {
            System.out.println("Bloqueado! Token desatualizado para o usuário: " + login);
            return; 
        }
        
        var autenticacao = 
        		new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    // Método auxiliar para tirar a palavra "Bearer " e deixar apenas o código do token
    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
