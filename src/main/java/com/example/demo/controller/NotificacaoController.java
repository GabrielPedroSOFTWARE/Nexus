package com.example.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Notificacao;
import com.example.demo.model.Usuario;
import com.example.demo.service.NotificacaoService;

@RestController
public class NotificacaoController {
	
	@Autowired
	private NotificacaoService notificacaoService;

	// Retorna as notificações
    @GetMapping("/retornar-notificacoes-usuario")
    public List<Notificacao> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoService.retornarNotificacoesUsuario(usuario);
    }
    
    // Marca a notificação como lida
    @PatchMapping("/marcar-notificacao/{id}")
    public ResponseEntity<String> marcarNotificacaoComoLida(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario usuario) {
    	try {
    		notificacaoService.marcarNotificacaoComoLida(id, usuario);
    		return ResponseEntity.ok("Notificação marcada como lida");
    	} catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
}
