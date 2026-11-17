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
	public void marcarNotificacaoComoLida(UUID id, Usuario usuario) {
		
		Notificacao notificacao = notificacaoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
		
		if(!notificacao.getCpfUsuario().equals(usuario.getCpf())) {
			throw new RuntimeException("Você não tem autorização para marcar essa notificação como lida");
		}
    	
    	notificacao.setLida(true);
    	
    	notificacaoRepository.save(notificacao);
	}
	
	
	// Retorna as notificações do usuário
	public List<Notificacao> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoRepository.findByCpfUsuario(usuario.getCpf());
    }
	
	
	// Envia a notificação de alteração do status para o usuário
	public void enviarNotificacao(String cpfUsuario, int status) {
	    Notificacao aviso = new Notificacao();
	    aviso.setCpfUsuario(cpfUsuario);
	    aviso.setStatus(status);
	    notificacaoRepository.save(aviso);
	}
}
