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

        if (token != null) {
            try {
                boolean tokenValido = verificarToken(token);
                if (!tokenValido) {
                    responderNaoAutorizado(response, "Token JWT inválido ou desatualizado.");
                    return;
                }
            } catch (RuntimeException ex) {
                responderNaoAutorizado(response, "Token JWT inválido ou expirado.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
    
    
    private void responderNaoAutorizado(HttpServletResponse response, String mensagem) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\": \"" + mensagem + "\"}");
    }

    // Verifica o token
    private boolean verificarToken(String token) {
        String cpf = tokenService.validarToken(token);
        if (cpf == null) return false;

        Usuario usuario = (Usuario) usuarioRepository.findByCpf(cpf);
        if (usuario == null) return false;

        Integer versaoNoToken = tokenService.extrairVersaoToken(token);
        if (versaoNoToken == null || !versaoNoToken.equals(usuario.getVersaoToken())) {
            System.out.println("Bloqueado! Token desatualizado para o usuário: " + cpf);
            return false;
        }

        var autenticacao =
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
        return true;
    }

    // Método auxiliar para tirar a palavra "Bearer " e deixar apenas o código do token
    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
