package com.example.demo.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    // Substitua pelo nome da classe que você usou para gerar o token no login
    @Autowired
    private TokenService tokenService; 

    // Substitua pelo nome do seu repositório de utilizadores
    @Autowired
    private UsuarioRepository usuarioRepository; 

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. Pega o token da requisição
        String token = recuperarToken(request);
        
        if (token != null) {
            // 2. Valida o token e extrai o login (ou email) de dentro dele
            String login = tokenService.validarToken(token); 
            
            if (login != null) {
                // 3. Vai ao banco de dados procurar quem é este utilizador
                UserDetails usuario = usuarioRepository.findByLogin(login); 
                
                if (usuario != null) {
                    // 4. Cria a autorização e avisa o Spring: "Este utilizador está autenticado, pode deixá-lo passar!"
                    var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        }
        
        // 5. Continua o fluxo normal da requisição (deixa entrar ou bloqueia dependendo da etapa anterior)
        filterChain.doFilter(request, response);
    }

    // Método auxiliar para tirar a palavra "Bearer " e deixar apenas o código do token
    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}
