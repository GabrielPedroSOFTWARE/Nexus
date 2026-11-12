package com.example.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Notificacao;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoRepository;

@RestController
public class NotificacaoController {
	
	@Autowired
	private NotificacaoRepository notificacaoRepository;

	// Retorna as notificações
    @GetMapping("/retornar-notificacoes-usuario")
    public List<Notificacao> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	String idUsuario = usuario.getId();
    	return notificacaoRepository.findByIdUsuario(idUsuario);
    }
    
    // Marca a notificação como lida
    @PatchMapping("/marcar-notificacao/{id}")
    public String marcarNotificacaoComoLida(@PathVariable("id") UUID id) {
    	
    	Notificacao notificacao = notificacaoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
    	
    	notificacao.setLida(true);
    	
    	notificacaoRepository.save(notificacao);
    	
    	return "Notificação marcada como lida";
         
    }
}
