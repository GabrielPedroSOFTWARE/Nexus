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

import com.example.demo.model.NotificacaoBackOffice;
import com.example.demo.model.NotificacaoUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.service.NotificacaoBackOfficeService;
import com.example.demo.service.NotificacaoUsuarioService;

@RestController
public class NotificacaoController {
	
	@Autowired
	private NotificacaoUsuarioService notificacaoUsuarioService;
	@Autowired
	private NotificacaoBackOfficeService notificacaoBackOfficeService;

	// Retorna as notificações do usuário
    @GetMapping("/user/retornar-notificacoes-usuario")
    public List<NotificacaoUsuario> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoUsuarioService.retornarNotificacoesUsuario(usuario);
    }
    

    // Retorna as notificações do técnico
    @GetMapping("/backoffice/retornar-notificacoes-backoffice")
    public List<NotificacaoBackOffice> retornarNotificacoesTecnico(@AuthenticationPrincipal Usuario backOffice) {
    	return notificacaoBackOfficeService.retornarNotificacoesNovoPedido(backOffice);
    }
    
    
    // Marca a notificação do usuário como lida
    @PatchMapping("/user/marcar-notificacao-usuario/{id}")
    public ResponseEntity<String> marcarNotificacaoUsuarioComoLida(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario usuario) {
    	try {
    		notificacaoUsuarioService.marcarNotificacaoUsuarioComoLida(id, usuario);
    		return ResponseEntity.ok("Notificação marcada como lida");
    	} catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    
    // Marca a notificação do técnico como lida
    @PatchMapping("/backoffice/marcar-notificacao-backoffice/{id}")
    public ResponseEntity<String> marcarNotificacaoTecnicoComoLida(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario tecnico) {
    	try {
    		notificacaoBackOfficeService.marcarNotificacaoTecnicoComoLida(tecnico, id);
    		return ResponseEntity.ok("Notificação marcada como lida");
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
}
