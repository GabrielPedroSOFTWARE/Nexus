package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.NotificacaoUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoUsuarioRepository;

@Service
public class NotificacaoUsuarioService {

	@Autowired
	private NotificacaoUsuarioRepository notificacaoRepository;
	
	
	// Marca a notificação do usuário como lida
	public void marcarNotificacaoUsuarioComoLida(UUID id, Usuario usuario) {
		
		NotificacaoUsuario notificacao = notificacaoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
		
		if(!notificacao.getCpfUsuario().equals(usuario.getCpf())) {
			throw new RuntimeException("Você não tem autorização para marcar essa notificação como lida");
		}
    	
    	notificacao.setLida(true);
    	
    	notificacaoRepository.save(notificacao);
	}
	
	
	// Retorna as notificações do usuário
	public List<NotificacaoUsuario> retornarNotificacoesUsuario(Usuario usuario) {
    	return notificacaoRepository.findByCpfUsuario(usuario.getCpf());
    }
	
	
	// Envia a notificação de alteração do status para o usuário
	public void enviarNotificacaoUsuario(String cpfUsuario, int status, LocalDate prazo) {
	    NotificacaoUsuario aviso = new NotificacaoUsuario();
	    aviso.setCpfUsuario(cpfUsuario);
	    aviso.setStatus(status);
	    if(prazo != null) aviso.setPrazo(prazo);
	    notificacaoRepository.save(aviso);
	}
}