package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.model.NotificacaoBackOffice;
import com.example.demo.model.Pedido;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoBackOfficeRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class NotificacaoBackOfficeService {
	
	@Autowired
	private NotificacaoBackOfficeRepository notificacaoBackOfficeRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private PedidoRepository pedidoRepository;
	

	// Marca a notificação do back office como lida
	public void marcarNotificacaoTecnicoComoLida(Usuario backOffice) {
		NotificacaoBackOffice notificacao = 
			notificacaoBackOfficeRepository.findByCpfTecnico(backOffice.getCpf());

		if(!notificacao.getCpfTecnico().equals(backOffice.getCpf())) {
			throw new RuntimeException("Você não tem autorização para marcar essa notificação como lida");
		}
			
		notificacao.setLida(true);
		
		notificacaoBackOfficeRepository.save(notificacao);
	}
	
	
	// Verifica a cada 1 hora se existe uma O.S em que o prazo está chegando 
	@Scheduled(fixedRate = 3600000)
	public void verificarPedidosProximosDoPrazo() {
		LocalDate hoje = LocalDate.now();
		LocalDate limite72Horas = hoje.plusDays(3);
		
	    List<Pedido> pedidos = pedidoRepository
	    		.findByStatusNotInAndAlertaPrazoEnviadoFalseAndDataFinalManutencaoBetween(
	    				List.of(5, 6), hoje, limite72Horas);
		    
		if(pedidos.isEmpty()) return;
		
		enviarNotificacaoLembrete(pedidos);
	}
	
	
	// Retorna a notificação para os back officers
	public List<NotificacaoBackOffice> retornarNotificacoesNovoPedido(Usuario backOffice) {
		return notificacaoBackOfficeRepository
				.findByCpfTecnicoAndLidaFalseOrderByDataCriacaoDesc(backOffice.getCpf());
    }
	
	
	// Envia um lembrete para o técnico responsável que o prazo está chegando ao fim
	public void enviarNotificacaoLembrete(List<Pedido> pedidos) {
		List<NotificacaoBackOffice> listaParaSalvar = new ArrayList<>();
			
		for (Pedido pedido : pedidos) {
	        pedido.setAlertaPrazoEnviado(true);
	        NotificacaoBackOffice aviso = new NotificacaoBackOffice();
            aviso.setCpfTecnico(pedido.getCpfTecnico()); 
            aviso.setIdPedido(pedido.getId());
            aviso.setMensagem("Você tem uma O.S que está quase chegando no prazo");
            listaParaSalvar.add(aviso);
	        pedidoRepository.save(pedido);
	    }
			
		notificacaoBackOfficeRepository.saveAll(listaParaSalvar);
	}
		
	
	// Envia a notificação de que uma nova O.S apareceu para todo o back office
	public void enviarNotificacaoNovoPedido(Pedido pedido, String mensagem) {
        List<Usuario> backOfficers = 
        		usuarioRepository.findByPerfilNotAndStatus(PerfilUsuario.USER, "Ativo");

        List<NotificacaoBackOffice> listaParaSalvar = new ArrayList<>();

        for (Usuario backOffice : backOfficers) {
            NotificacaoBackOffice aviso = new NotificacaoBackOffice();
            aviso.setCpfTecnico(backOffice.getCpf()); 
            aviso.setIdPedido(pedido.getId());
            aviso.setMensagem(mensagem);
            listaParaSalvar.add(aviso);
        }

        notificacaoBackOfficeRepository.saveAll(listaParaSalvar);
    }
}
