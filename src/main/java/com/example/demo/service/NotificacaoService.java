package com.example.demo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import com.example.demo.model.Notificacao;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoRepository;

@Service
public class NotificacaoService {

	@Autowired
	private NotificacaoRepository notificacaoRepository;
	
	
	// Marca a notificação como lida
	public void marcarNotificacaoComoLida(UUID id) {
		Notificacao notificacao = notificacaoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
    	
    	notificacao.setLida(true);
    	
    	notificacaoRepository.save(notificacao);
	}
	
	
	// Retorna as notificações do usuário
	public List<Notificacao> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoRepository.findByIdUsuario(usuario.getId());
    }
	
	
	// Envia a notificação de alteração do status para o usuário
	public void enviarNotificacao(String idUsuario, int status) {
	    Notificacao aviso = new Notificacao();
	    aviso.setIdUsuario(idUsuario);
	    aviso.setStatus(status);
	    notificacaoRepository.save(aviso);
	}
}
