package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.AtualizacaoUsuarioDTO;
import com.example.demo.model.DadosLogin;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.service.AutenticacaoService;
import com.example.demo.service.UsuarioService;

@RestController
public class AutenticacaoController {

	@Autowired
	private AutenticacaoService autenticacaoService;
	@Autowired
	private UsuarioService usuarioService;

	
    // Realiza o login
    @PostMapping("/login")
    public ResponseEntity<?> efetuarLogin(@RequestBody DadosLogin dados) {
        try {
        	return autenticacaoService.efetuarLogin(dados);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Login ou senha errados");
        }
    }
    
    
    // Cadastro temporário
    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrarUsuario(@RequestBody Usuario novoUsuario) {
    	usuarioService.cadastrarUsuario(novoUsuario);
        return ResponseEntity.ok("Solicitação enviada com sucesso!");
    }
    
    
    // Mostra os usuários pendentes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/usuarios-pendentes")
    public List<Usuario> verificarPendentes() {
        return usuarioService.verificarPendentes();
    }
    
    
    // Aceita a solicitação de cadastro
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/aceitar-sollicitacao/{id}")
    public ResponseEntity<String> atualizarStatusUsuario(
            @PathVariable("id") String id,
            @RequestBody PerfilUsuario perfilDefinido) {
    	usuarioService.atualizarStatusUsuario(id, perfilDefinido);        
        return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: Ativo");
    }
    
    
    // Nega a solicitação de cadastro do usuário
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/negar-solicitacao-usuario/{id}")
    public ResponseEntity<String> negarSolicitacaoUsuario(@PathVariable("id") String id) {
    	usuarioService.negarSolicitacaoUsuario(id);
    	return ResponseEntity.ok("Solicitação negada");
    }
    
    
 // Recebe uma lista com as alterações feitas na tabela
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/salvar-alteracoes-usuarios")
    public ResponseEntity<String> salvarAlteracoesUsuarios(
            @RequestBody List<AtualizacaoUsuarioDTO> alteracoes) {
        usuarioService.salvarAlteracoesEmLote(alteracoes);
        return ResponseEntity.ok("Todos os usuários foram atualizados com sucesso!");
    }
    
    // Retorna todos os usuários cadastrados, com excessão do admin atual
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/retornar-todos-menos-atual")
    public List<Usuario> retornarUsuarios(@AuthenticationPrincipal Usuario usuarioLogado) {
    	return usuarioService.retornarUsuarios(usuarioLogado);
    }
}