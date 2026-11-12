package com.example.demo.service;

import java.io.IOException;
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

@Service
public class PedidoService {
	
	@Autowired
    private PedidoRepository pedidoRepository;
	@Autowired
	private NotificacaoService notificacaoService;
	@Autowired
	private PrioridadeService prioridadeService;
	
	
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

	
	// Cancela a O.S
    public void cancelarPedido(UUID id, Usuario usuario, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getId().equals(pedido.getIdTecnico())) 
            throw new RuntimeException("Sem autorização para alterar o status");
        
        pedido.setStatus(6);
        pedido.setJustificativaCancelamento(justificativa);
        
        pedidoRepository.save(pedido);
    }
    
    
    // Altera o status da O.S
    public void alterarStatusPedido(UUID id, Map<String, String> dados, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getId().equals(pedido.getIdTecnico())) 
        	throw new RuntimeException("Sem autorização para alterar o status");
        
        int status = Integer.parseInt(dados.get("novoStatusId"));
        pedido.setStatus(status);
        
        if (status == 1) pedido.setObservacoesTecnico(dados.get("observacao"));
        if (status == 5) pedido.setDescricao(dados.get("descricao"));
        
        notificacaoService.enviarNotificacao(pedido.getIdUsuario(), status); 
        
        pedidoRepository.save(pedido);
    }
    
    
    // Atribui um técnico a uma O.S
    public void atribuirTecnico(UUID id, String idTecnico) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	pedido.setIdTecnico(idTecnico);
    	pedidoRepository.save(pedido);
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
    
    
    // Lista todas as O.Ss 
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoRepository.findAll();
    }
    
}
