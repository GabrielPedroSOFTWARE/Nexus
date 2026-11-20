package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;

import com.example.demo.model.NotificacaoTecnico;
import com.example.demo.model.NotificacaoUsuario;
import com.example.demo.model.Pedido;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoTecnicoRepository;
import com.example.demo.repository.NotificacaoUsuarioRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class NotificacaoService {

	@Autowired
	private NotificacaoUsuarioRepository notificacaoRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private NotificacaoTecnicoRepository notificacaoTecnicoRepository;
	
	
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
	
	
	// Marca a notificação do técnico como lida
	public void marcarNotificacaoTecnicoComoLida(Usuario tecnico) {
		NotificacaoTecnico notificacao = notificacaoTecnicoRepository.findByCpfTecnico(tecnico.getCpf());

		if(!notificacao.getCpfTecnico().equals(tecnico.getCpf())) {
			throw new RuntimeException("Você não tem autorização para marcar essa notificação como lida");
		}
		
		notificacao.setLida(true);
		
		notificacaoTecnicoRepository.save(notificacao);
		
	}
	
	
	// Retorna as notificações do usuário
	public List<NotificacaoUsuario> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	return notificacaoRepository.findByCpfUsuario(usuario.getCpf());
    }
	
	// Retorna as notificações do técnico
	public List<NotificacaoTecnico> retornarNotificacoesTecnico(Usuario usuarioLogado) {
        return notificacaoTecnicoRepository
                .findByCpfTecnicoAndLidaFalseOrderByDataCriacaoDesc(usuarioLogado.getCpf());
    }
	
	
	// Envia a notificação de alteração do status para o usuário
	public void enviarNotificacao(String cpfUsuario, int status, LocalDate prazo) {
	    NotificacaoUsuario aviso = new NotificacaoUsuario();
	    aviso.setCpfUsuario(cpfUsuario);
	    aviso.setStatus(status);
	    if(prazo != null) aviso.setPrazo(prazo);
	    notificacaoRepository.save(aviso);
	}
	
	// Envia a notificação de que uma nova O.S apareceu para todos os técnicos
	public void enviarNotificacaoParaTecnicos(Pedido pedido) {
        List<Usuario> tecnicos = usuarioRepository.findByPerfilAndStatus(PerfilUsuario.SUPPORT, "Ativo");

        List<NotificacaoTecnico> listaParaSalvar = new ArrayList<>();

        for (Usuario tecnico : tecnicos) {
            NotificacaoTecnico aviso = new NotificacaoTecnico();
            aviso.setCpfTecnico(tecnico.getCpf()); 
            aviso.setIdPedido(pedido.getId());
            aviso.setMensagem("Nova O.S. aberta!");
            listaParaSalvar.add(aviso);
        }

        notificacaoTecnicoRepository.saveAll(listaParaSalvar);
    }
}
