package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AutenticacaoService;
import com.example.demo.service.TokenService;

@RestController
public class AutenticacaoController {

	@Autowired
    private AuthenticationManager manager;
	
	@Autowired
	private AutenticacaoService autenticacaoService;

    @Autowired
    private TokenService tokenService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encriptador;

    // Realiza o login
    @PostMapping("/login")
    public ResponseEntity<?> efetuarLogin(@RequestBody DadosLogin dados) { 
        
        ResponseEntity<?> erroDeStatus = 
        		autenticacaoService.verificarStatusUsuario(dados.login());
        
        if (erroDeStatus != null) return erroDeStatus;
        
        try {
            Usuario logado = (Usuario) manager.authenticate(
                new UsernamePasswordAuthenticationToken(dados.login(), dados.senha())
            ).getPrincipal();
            return ResponseEntity.ok(Map.of(
                "token", tokenService.gerarToken(logado),
                "perfil", logado.getPerfil().name(),
                "admin", String.valueOf(logado.isEhAdmin())
            ));
            
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Login ou senha errados");
        }
    }
    
    // Cadastro temporário
    @PostMapping("/cadastrar")
    public String cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaSegura = encriptador.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaSegura);
        novoUsuario.setDataHoraCadastro(LocalDateTime.now());
        usuarioRepository.save(novoUsuario);
        
        return "Solicitação enviada com sucesso!";
    }
    
    // Mostra os usuários pendentes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/usuarios-pendentes")
    public List<Usuario> verificarPendentes() {
        return usuarioRepository.findByStatus("Aguardando análise");
    }
    
    // Aceita a solicitação de cadastro
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/aceitar-sollicitacao/{id}")
    public ResponseEntity<String> atualizarStatusUsuario(
            @PathVariable("id") String id,
            @RequestBody PerfilUsuario perfilDefinido) {
        
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        
        Usuario usuario = usuarioOptional.get();
        
        usuario.setStatus("Ativo");
        usuario.setPerfil(perfilDefinido);
        
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: Ativo");
    }
    
    // Nega a solicitação de cadastro do usuário
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/negar-solicitacao-usuario/{id}")
    public String negarSolicitacaoUsuario(@PathVariable("id") String id) {
    	
    	Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if(!usuario.getStatus().equals("Aguardando análise")) {
        	return "Usuário já está cadastrado";
        }
        
    	usuarioRepository.deleteById(id);
    	return "Solicitação negada";
    }
    
    // Altera o status da conta
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/alterar-status-conta/{id}")
    public ResponseEntity<String> alterarStatusConta(@PathVariable("id") String id) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
    	
    	if(usuario.getStatus().equals("Ativo")) usuario.setStatus("Inativo");
    		else usuario.setStatus("Ativo");
    	
    	usuarioRepository.save(usuario);
    	
    	return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: "
    	+ usuario.getStatus());	
    }
    
    // Altera a permissão de admin da conta
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/alterar-permissao-admin/{id}")
    public ResponseEntity<String> alterarPermissaoAdmin(@PathVariable("id") String id) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
        
        if(usuario.isEhAdmin()) usuario.setEhAdmin(false);
        	else usuario.setEhAdmin(true);
        
        usuarioRepository.save(usuario);
        
        String situacaoAdminUsuario = (usuario.isEhAdmin()) ? 
        		"é admin" : "não é mais admin";
        
        return ResponseEntity.ok("Situação de admin do usuário: " + situacaoAdminUsuario);
    }
    
    // Altera o perfil da conta
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/alterar-perfil-conta/{id}")
    public ResponseEntity<String> alterarPerfilConta(@PathVariable("id") String id,
    		@RequestBody PerfilUsuario perfil) {
    	Optional<Usuario> usuarioOptional = usuarioRepository.findById(id);
        Usuario usuario = usuarioOptional.get();
        
        usuario.setPerfil(perfil);
        usuario.setVersaoToken(usuario.getVersaoToken() + 1);
        
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok("Perfil da conta alterado para " + perfil.toString());
    }
    
    // Retorna todos os usuários cadastrados, com excessão do admin atual
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/retornar-todos-menos-atual")
    public List<Usuario> retornarUsuarios(@AuthenticationPrincipal Usuario usuarioLogado) {
    	return usuarioRepository.findByIdNot(usuarioLogado.getId());
    }
    
}

// Criação de classe de dados
record DadosLogin(String login, String senha) {}