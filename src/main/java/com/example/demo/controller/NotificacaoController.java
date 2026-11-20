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

import com.example.demo.model.NotificacaoTecnico;
import com.example.demo.model.NotificacaoUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.service.NotificacaoService;

@RestController
public class NotificacaoController {
	
	@Autowired
	private NotificacaoService notificacaoService;

	// Retorna as notificações do usuário
    @GetMapping("/user/retornar-notificacoes-usuario")
    public List<NotificacaoUsuario> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoService.retornarNotificacoesUsuario(usuario);
    }
    

    // Retorna as notificações do técnico
    @GetMapping("/backoffice/retornar-notificacoes-tecnico")
    public List<NotificacaoTecnico> retornarNotificacoesTecnico(@AuthenticationPrincipal Usuario tecnico) {
    	return notificacaoService.retornarNotificacoesTecnico(tecnico);
    }
    
    
    // Marca a notificação do usuário como lida
    @PatchMapping("/user/marcar-notificacao-usuario/{id}")
    public ResponseEntity<String> marcarNotificacaoUsuarioComoLida(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario usuario) {
    	try {
    		notificacaoService.marcarNotificacaoUsuarioComoLida(id, usuario);
    		return ResponseEntity.ok("Notificação marcada como lida");
    	} catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    
    // Marca a notificação do técnico como lida
    @PatchMapping("/backoffice/marcar-notificacao-tecnico/{id}")
    public ResponseEntity<String> marcarNotificacaoTecnicoComoLida(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario tecnico) {
    	try {
    		notificacaoService.marcarNotificacaoTecnicoComoLida(tecnico);
    		return ResponseEntity.ok("Notificação marcada como lida");
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
}
