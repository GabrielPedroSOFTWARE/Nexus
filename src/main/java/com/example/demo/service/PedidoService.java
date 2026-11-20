package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class PedidoService {
	
	@Autowired
    private PedidoRepository pedidoRepository;
	@Autowired
	private NotificacaoService notificacaoService;
	@Autowired
	private PrioridadeService prioridadeService;
	@Autowired
	private HistoricoPedidoService historicoPedidoService;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private EquipamentoRepository equipamentoRepository;
	
	private List<String> status = List.of("Recebida", "Em diagnóstico",
			"Em manutenção", "Aguardando peças", "Pronto para retirada",
			"Concluída", "Cancelada");
	
	private DateTimeFormatter formatadorDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	
	
	// Cria uma nova O.S
	public void criarNovoPedido(Pedido novoPedido, MultipartFile arquivoFoto, Usuario usuario) 
			throws IOException  {
		
		boolean fazParte = equipamentoRepository
    			.verificarPossivelExclusaoEquipamento(novoPedido.getIdPC(), usuario.getCpf());
		
		if(!fazParte) {
			throw new RuntimeException("Há uma O.S em aberto para esse equipamento");
		}
		
		if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
			
            novoPedido.setFoto(arquivoFoto.getBytes());
            novoPedido.setNomeArquivoFoto(arquivoFoto.getOriginalFilename());
            novoPedido.setTipoArquivoFoto(arquivoFoto.getContentType());
        }
		
        novoPedido.setDataInicioManutencao(LocalDate.now());
        novoPedido.setStatus(0);
        novoPedido.setCpfUsuario(usuario.getCpf());
        novoPedido.setPrioridade(prioridadeService.sugerirPrioridadeInicial(novoPedido));
        
        pedidoRepository.save(novoPedido);
        
        notificacaoService.enviarNotificacaoParaTecnicos(novoPedido);
        
        String alteracao = "O.S criada";
        historicoPedidoService.registrarAcao(novoPedido, usuario.getCpf(), alteracao);
	}

	
	// Altera uma O.S
	public void alterarPedido(Pedido novoPedido, MultipartFile arquivoFoto, UUID id, Usuario usuario)
	throws IOException {
		Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
		
		if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfUsuario())) {
			throw new RuntimeException("Você não tem permissão para alterar essa O.S");
		}
		
		if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
		
		if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
			
            novoPedido.setFoto(arquivoFoto.getBytes());
            novoPedido.setNomeArquivoFoto(arquivoFoto.getOriginalFilename());
            novoPedido.setTipoArquivoFoto(arquivoFoto.getContentType());
        }
		novoPedido.setId(id);
		novoPedido.setCpfUsuario(pedido.getCpfUsuario());
		novoPedido.setDataInicioManutencao(pedido.getDataInicioManutencao());
		novoPedido.setPrioridade(prioridadeService.sugerirPrioridadeInicial(novoPedido));
		pedidoRepository.save(novoPedido);
		String alteracao = "Mudança dos dados da O.S";
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
	}
	
	// Cancela a O.S
    public void cancelarPedido(UUID id, Usuario usuario, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para cancelar a O.S");
        }
        
        if(pedido.getStatus() == 6) {
        	throw new RuntimeException("A O.S já foi cancelada");
        }
        
        pedido.setStatus(6);
        pedido.setJustificativaCancelamento(justificativa);
        
        pedidoRepository.save(pedido);
        String alteracao = "O.S cancelada";
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    }
    
    
    // Altera o status da O.S
    public void alterarStatusPedido(UUID id, Map<String, String> dados, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	int statusAntigo = pedido.getStatus();

    	if(List.of(5, 6).contains(statusAntigo)) {
    		throw new RuntimeException("A O.S já foi " + status.get(statusAntigo));
    	}
    	
    	if(pedido.getCpfTecnico() == null) {
    		throw new RuntimeException("Defina primeiro um técnico"
    				+ " antes de alterar o status da O.S");
    	}
        
        if(pedido.getCpfTecnico() != null && !usuario.isEhAdmin()
        		&& !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para alterar o status da O.S");
        }
        
        
        int statusNovo = Integer.parseInt(dados.get("novoStatusId"));
        if(statusNovo == 0) {
        	throw new RuntimeException("Status só pode voltar para o início ao devolvê-lo à fila");
        }
        pedido.setStatus(statusNovo);
        
        LocalDate novoPrazo = null;
        
        if(statusAntigo == 0 && statusNovo == 1) pedido.setObservacoesTecnico(dados.get("observacao"));
        else if(statusAntigo < 2 && statusNovo == 2) {
        	novoPrazo = LocalDate.parse(dados.get("prazo"), formatadorDataHora);
        	pedido.setDataFinalManutencao(novoPrazo);
        }
        else if(statusAntigo > 1 && statusNovo == 1) {
        	novoPrazo = LocalDate.parse(dados.get("prazo"), formatadorDataHora);
        	pedido.setDataFinalManutencao(LocalDate.parse(dados.get("prazo"), formatadorDataHora));
        }
        else if (statusNovo == 5) {
        	pedido.setObservacoesTecnico("observacao");
        	pedido.setDescricao(dados.get("descricao"));
        } else if(statusAntigo > statusNovo) pedido.setJustificativa("justificativa");
        
        notificacaoService.enviarNotificacao(pedido.getCpfUsuario(), statusNovo, novoPrazo);
        String alteracao = "Status: " +
        		status.get(statusAntigo) + " -> " + status.get(statusNovo);
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
        
        pedidoRepository.save(pedido);
    }
    
    
    // Atribui um técnico a uma O.S
    public void atribuirTecnico(UUID id, String cpfTecnico, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
        
    	cpfTecnico = cpfTecnico.replaceAll("\\D", "");
        Usuario novoTecnico = usuarioRepository.findById(cpfTecnico)
                .orElseThrow(() -> new RuntimeException("Novo técnico não encontrado"));

        String alteracao = "Técnico atribuído: " + novoTecnico.getNome();

        if (pedido.getCpfTecnico() != null) {
            Usuario antigoTecnico = usuarioRepository.findById(pedido.getCpfTecnico())
                    .orElseThrow(() -> new RuntimeException("Técnico anterior não encontrado"));
            alteracao = "Técnico: " + antigoTecnico.getNome() + " -> " + novoTecnico.getNome();
        }

        pedido.setCpfTecnico(cpfTecnico);
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
        pedidoRepository.save(pedido);
    }
    
    
    // Devolve a O.S para a fila
    public void devolverParaAFila(UUID id, Usuario usuario, String justificativa) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
    	
    	pedido.setCpfTecnico(null);
    	pedido.setStatus(0);
    	
    	String alteracao = "O.S de volta à fila\n"
    			+ "Justificativa: " + justificativa;
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    }
    
    
    // Definir/Alterar a justificativa de uma O.S
    public void definirJustificativa(UUID id, String justificativa, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para alterar o status da O.S");
        }
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
    	
    	String antigaJustificativa = pedido.getJustificativa();
    	
    	pedido.setJustificativaCancelamento(justificativa);
    	
    	String alteracao = (antigaJustificativa != null && !antigaJustificativa.isEmpty()) ?
    			"Alteração de justificativa" : "Definição de justificativa";
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    	pedidoRepository.save(pedido);
    }
    
    
    

    
    // Lista as O.Ss por equipamento
    public List<Pedido> listarManutencoesEquipamento(Long id) {
    	return pedidoRepository.findByIdPC(id);
    }
    
    
    // Lista as O.Ss por usuário
    public List<Pedido> listarManutencoesUsuario (Usuario usuarioLogado) {
		return pedidoRepository.findByCpfUsuario(usuarioLogado.getCpf());
	}
    
    
    // Lista as O.Ss por técnico
    public List<Pedido> listarManutencoesAtribuidas(Usuario usuarioLogado) {
    	return pedidoRepository.findByCpfTecnicoAndStatusNotIn(
                usuarioLogado.getCpf(),
                List.of(5, 6)
        );
    }
    
    
    // Lista as O.Ss por frequência
    public List<Pedido> listarManutencoesPorFrequencia(int frequencia) {
    	return pedidoRepository.findByFrequencia(frequencia);
    }
    
    // Lista todas as O.Ss 
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoRepository.findAll();
    }
    
}
