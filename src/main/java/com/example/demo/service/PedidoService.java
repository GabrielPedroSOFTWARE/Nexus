package com.example.demo.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
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
	
	private List<String> status = List.of("Recebida", "Em diagnóstico",
			"Em manutenção", "Aguardando peças", "Pronto para retirada",
			"Concluída");
	
	
	
	// Cria uma nova O.S
	public void criarNovoPedido(Pedido novoPedido, MultipartFile arquivoFoto, Usuario usuario) 
			throws IOException  {
		if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
			
            novoPedido.setFoto(arquivoFoto.getBytes());
            novoPedido.setNomeArquivoFoto(arquivoFoto.getOriginalFilename());
            novoPedido.setTipoArquivoFoto(arquivoFoto.getContentType());
        }
		
        novoPedido.setDataInicioManutencao(LocalDate.now());
        novoPedido.setStatus(0);
        novoPedido.setIdUsuario(usuario.getId());
        novoPedido.setPrioridade(prioridadeService.sugerirPrioridadeInicial(novoPedido));
        
        pedidoRepository.save(novoPedido);
		
	}

	
	// Altera uma O.S
	public void alterarPedido(Pedido novoPedido, MultipartFile arquivoFoto, UUID id, Usuario usuario)
	throws IOException {
		Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
		
		if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
			
            novoPedido.setFoto(arquivoFoto.getBytes());
            novoPedido.setNomeArquivoFoto(arquivoFoto.getOriginalFilename());
            novoPedido.setTipoArquivoFoto(arquivoFoto.getContentType());
        }
		novoPedido.setId(id);
		novoPedido.setIdUsuario(pedido.getIdUsuario());
		novoPedido.setDataInicioManutencao(pedido.getDataInicioManutencao());
		novoPedido.setPrioridade(prioridadeService.sugerirPrioridadeInicial(novoPedido));
		pedidoRepository.save(novoPedido);
		String alteracao = "Mudança dos dados da O.S";
        historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
	}
	
	// Cancela a O.S
    public void cancelarPedido(UUID id, Usuario usuario, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getId().equals(pedido.getIdTecnico())) 
            throw new RuntimeException("Sem autorização para alterar o status");
        
        pedido.setStatus(6);
        pedido.setJustificativaCancelamento(justificativa);
        
        pedidoRepository.save(pedido);
        String alteracao = "O.S cancelada";
        historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
    }
    
    
    // Altera o status da O.S
    public void alterarStatusPedido(UUID id, Map<String, String> dados, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getId().equals(pedido.getIdTecnico())) 
        	throw new RuntimeException("Sem autorização para alterar o status");
        
        int statusAntigo = pedido.getStatus();
        
        int statusNovo = Integer.parseInt(dados.get("novoStatusId"));
        pedido.setStatus(statusNovo);
        
        if (statusNovo == 1) pedido.setObservacoesTecnico(dados.get("observacao"));
        if (statusNovo == 5) pedido.setDescricao(dados.get("descricao"));
        
        notificacaoService.enviarNotificacao(pedido.getIdUsuario(), statusNovo);
        String alteracao = "Status: " +
        		status.get(statusAntigo) + " -> " + status.get(statusNovo);
        historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
        
        pedidoRepository.save(pedido);
    }
    
    
    // Atribui um técnico a uma O.S
    public void atribuirTecnico(UUID id, String idTecnico, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	Usuario tecnicoAntigo = usuarioRepository.findById(pedido.getIdTecnico())
    			.orElse(null);
    	
    	Usuario novoTecnico = usuarioRepository.findById(idTecnico)
    			.orElseThrow(() -> new RuntimeException("Novo técnico não encontrado"));
    	
    	pedido.setIdTecnico(idTecnico);
    	
    	String alteracao = (tecnicoAntigo != null) ? 
    			"Técnico: " + tecnicoAntigo.getNome() + " -> " + novoTecnico.getNome() :
    				"Técnico atribuído: " + novoTecnico.getNome();
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
    	
    	pedidoRepository.save(pedido);
    }
    
    
    // Define/Altera a justificativa de uma O.S
    public void definirJustificativa(UUID id, String justificativa, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	String antigaJustificativa = pedido.getJustificativa();
    	
    	pedido.setJustificativaCancelamento(justificativa);
    	
    	String alteracao = (antigaJustificativa != null && !antigaJustificativa.isEmpty()) ?
    			"Alteração de justificativa" : "Definição de justificativa";
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
    	
    	pedidoRepository.save(pedido);
    }
    
    
    // Define o prazo de uma O.S
    public void definirPrazo(UUID id, LocalDate prazo, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	pedido.setDataFinalManutencao(prazo);
    	
    	var formatador = new SimpleDateFormat("dd/mm/yyyy");
    	
    	String alteracao = "Prazo estimado definido: " + formatador.format(prazo);
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getId(), alteracao);
    }

    
    // Lista as O.Ss por equipamento
    public List<Pedido> listarManutencoesEquipamento(Long id) {
    	return pedidoRepository.findByIdPC(id);
    }
    
    
    // Lista as O.Ss por usuário
    public List<Pedido> listarManutencoesUsuario (Usuario usuarioLogado) {
		return pedidoRepository.findByIdUsuario(usuarioLogado.getId());
	}
    
    
    // Lista as O.Ss por técnico
    public List<Pedido> listarManutencoesAtribuidas(Usuario usuarioLogado) {
    	return pedidoRepository.findByIdTecnico(usuarioLogado.getId());
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
