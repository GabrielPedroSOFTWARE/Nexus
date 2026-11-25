package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
	@Autowired
	private CriptografiaCpfService criptografiaService;
	

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
	
	
	// Verifica a cada 1 hora se existe uma O.S que está a 72 horas sem funcionar
	@Scheduled(fixedRate = 3600000)
	public void verificarPedidosSemAlteracao() {
	    LocalDateTime agora = LocalDateTime.now();
	    List<Integer> encerrados = List.of(5, 6);

	    // +24h após o aviso do técnico -> avisa os ADMs
	    List<Pedido> paraAdm = pedidoRepository
	            .buscarPedidosInativos(encerrados, 1, agora.minusHours(96));

	    if (!paraAdm.isEmpty()) {
	        List<Usuario> admins = usuarioRepository.findByEhAdminTrueAndStatus("Ativo");
	        enviarNotificacaoInatividadeParaAdm(paraAdm, admins);
	    }

	    // 72h sem nenhuma alteração na O.S -> avisa o técnico
	    List<Pedido> paraTecnico = pedidoRepository
	            .buscarPedidosInativos(encerrados, 0, agora.minusHours(72));

	    if(paraTecnico.isEmpty()) return;
	    
	    enviarNotificacaoInatividadeParaTecnico(paraTecnico);
	}
	
	
	// Retorna a notificação para os back officers
	public List<NotificacaoBackOffice> retornarNotificacoesNovoPedido(Usuario backOffice) {
	    List<NotificacaoBackOffice> notificacoes = notificacaoBackOfficeRepository
	            .findByCpfTecnicoAndLidaFalseOrderByDataCriacaoDesc(backOffice.getCpf());

	    if (!notificacoes.isEmpty()) {
	        String cpfMascarado = 
	        		criptografiaService.descriptografarEMascarar(backOffice.getCpf());

	        for (NotificacaoBackOffice notificacao : notificacoes) {
	            notificacao.setCpfTecnicoDescriptografado(cpfMascarado);
	        }
	    }

	    return notificacoes;
	}
	
	
	/* Envia uma notificação para o ADM 24 horas depois de mandar a
	notificação de inatividade para o técnico */
	public void enviarNotificacaoInatividadeParaAdm(List<Pedido> pedidos, List<Usuario> admins) {
		List<NotificacaoBackOffice> listaParaSalvar = new ArrayList<>();
		
		for (Pedido pedido : pedidos) {
			Usuario tecnico = (Usuario) usuarioRepository.findByCpf(pedido.getCpfTecnico());
            pedido.setNivelAlertaInatividade(2);
            for(Usuario admin : admins) {
            	NotificacaoBackOffice aviso = new NotificacaoBackOffice();
                aviso.setCpfTecnico(admin.getCpf());
                aviso.setIdPedido(pedido.getId());
                aviso.setMensagem("O técnico " + tecnico.getNome() +
                		" está a mais de 3 dias sem fazer alterações na " + pedido.getId());
                listaParaSalvar.add(aviso);
            }
            notificacaoBackOfficeRepository.saveAll(listaParaSalvar);
            pedidoRepository.save(pedido);
        }
		
	}
	
	
	// Envia uma notificação para o técnico responsável que ele ficou 72 horas sem atualizar a O.S
	public void enviarNotificacaoInatividadeParaTecnico(List<Pedido> pedidos) {
		List<NotificacaoBackOffice> listaParaSalvar = new ArrayList<>();
		
		for (Pedido pedido : pedidos) {
	        pedido.setNivelAlertaInatividade(1);
	        NotificacaoBackOffice aviso = new NotificacaoBackOffice();
	        aviso.setCpfTecnico(pedido.getCpfTecnico());
	        aviso.setIdPedido(pedido.getId());
	        aviso.setMensagem("Já se passaram três dias que você não fez nenhuma alteração na "
	        		+ pedido.getId());
	        listaParaSalvar.add(aviso);
	        pedidoRepository.save(pedido);
	    }
		
		notificacaoBackOfficeRepository.saveAll(listaParaSalvar);
	}
	
	
	// Envia um lembrete para o técnico responsável que o prazo está chegando ao fim
	public void enviarNotificacaoLembrete(List<Pedido> pedidos) {
		List<NotificacaoBackOffice> listaParaSalvar = new ArrayList<>();
			
		for (Pedido pedido : pedidos) {
	        pedido.setAlertaPrazoEnviado(true);
	        NotificacaoBackOffice aviso = new NotificacaoBackOffice();
            aviso.setCpfTecnico(pedido.getCpfTecnico()); 
            aviso.setIdPedido(pedido.getId());
            aviso.setMensagem("A " + pedido.getId() + " está quase chegando no prazo");
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
