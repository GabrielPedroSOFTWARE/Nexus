package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Notificacao;
import com.example.demo.repository.NotificacaoRepository;

@Service
public class NotificacaoService {

	@Autowired
	private NotificacaoRepository notificacaoRepository;
	
	// Envia a notificação de alteração do status para o usuário
	public void enviarNotificacao(String idUsuario, int status) {
	    Notificacao aviso = new Notificacao();
	    aviso.setIdUsuario(idUsuario);
	    aviso.setStatus(status);
	    notificacaoRepository.save(aviso);
	}
}
