package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.AtualizacaoUsuarioDTO;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;

@RestController
public class UsuarioController {
	
	@Autowired
	private UsuarioService usuarioService;

	
	// Mostra os usuários pendentes
    @GetMapping("/adm/usuarios-pendentes")
    public List<Usuario> verificarPendentes() {
        return usuarioService.verificarPendentes();
    }
    
    
    // Aceita a solicitação de cadastro
    @PutMapping("/adm/aceitar-sollicitacao")
    public ResponseEntity<String> atualizarStatusUsuario(
            @RequestBody String cpf,
            @RequestBody PerfilUsuario perfilDefinido) {
    	usuarioService.atualizarStatusUsuario(cpf, perfilDefinido);        
        return ResponseEntity.ok("Situação do usuário atualizada com sucesso para: Ativo");
    }
    
    
    // Nega a solicitação de cadastro do usuário
    @DeleteMapping("/adm/negar-solicitacao-usuario")
    public ResponseEntity<String> negarSolicitacaoUsuario(@RequestBody String cpf) {
    	usuarioService.negarSolicitacaoUsuario(cpf);
    	return ResponseEntity.ok("Solicitação negada");
    }
    
    
    // Recebe uma lista com as alterações feitas na tabela
    @PutMapping("/adm/salvar-alteracoes-usuarios")
    public ResponseEntity<String> salvarAlteracoesUsuarios(
            @RequestBody List<AtualizacaoUsuarioDTO> alteracoes) {
        usuarioService.salvarAlteracoesEmLote(alteracoes);
        return ResponseEntity.ok("Todos os usuários foram atualizados com sucesso!");
    }
    
    
    // Retorna todos os usuários cadastrados, com excessão do admin atual
    @GetMapping("/adm/retornar-todos-menos-atual")
    public List<Usuario> retornarUsuarios(@AuthenticationPrincipal Usuario usuarioLogado) {
    	return usuarioService.retornarUsuarios(usuarioLogado);
    }
}
