package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.TokenService;

@RestController
public class AutenticacaoController {

	@Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encriptador;

    // Realiza o login
    @PostMapping("/login")
    public ResponseEntity<?> efetuarLogin(@RequestBody DadosLogin dados) { 
        
        Usuario usuarioEncontrado = (Usuario) usuarioRepository.findByLogin(dados.login());
        
        if (usuarioEncontrado != null && "PENDENTE".equals(usuarioEncontrado.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acesso negado: Sua conta ainda está aguardando a aprovação de um administrador.");
        }
        
        UsernamePasswordAuthenticationToken token = 
                new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        
        Authentication autenticacao = manager.authenticate(token);
        
        Usuario usuarioLogado = (Usuario) autenticacao.getPrincipal();
        
        String tokenJWT = tokenService.gerarToken(usuarioLogado);
        
        Map<String, String> resposta = new HashMap<>();
        resposta.put("token", tokenJWT);
        resposta.put("perfil", usuarioLogado.getPerfil().name());
        
        return ResponseEntity.ok(resposta); 
    }
    
    // Cadastro temporário
    @PostMapping("/cadastrar")
    public String cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaSegura = encriptador.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaSegura);
        usuarioRepository.save(novoUsuario);
        
        return "Usuário registado com sucesso!";
    }
    
    // Mostra os usuários pendentes
//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/usuarios-pendentes")
    public List<Usuario> verificarPendentes() {
        return usuarioRepository.findByStatus("PENDENTE");
    }
    
    // Atualiza a situação do status do usuário
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/atualizar-status/{id}")
    public ResponseEntity<String> atualizarStatusUsuario(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> requestBody) {
        
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        
        Usuario usuario = usuarioOptional.get();
        
        String novoStatus = requestBody.get("status");
        
        if (novoStatus == null || novoStatus.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Erro: Tem de informar qual é o novo status.");
        }
        
        usuario.setStatus(novoStatus.toUpperCase());
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: " + novoStatus);
    }
}

// Criação de classe de dados
record DadosLogin(String login, String senha) {}