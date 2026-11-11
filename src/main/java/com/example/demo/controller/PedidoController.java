package com.example.demo.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Notificacao;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.NotificacaoRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.service.PrioridadeService;

@RestController
public class PedidoController {
	
	@Autowired
	private PrioridadeService prioridadeService;
	
	@Autowired
	private PedidoRepository pedidoRepository;
	@Autowired
	private NotificacaoRepository notificacaoRepository;
	
	// Salva a solicitação no banco de dados
    @PostMapping(value = "/criar-pedido", consumes = "multipart/form-data")
    public String criarNovoPedido(
            @ModelAttribute Pedido novoPedido, 
            @RequestParam(value = "arquivoImagem", required = false) MultipartFile arquivoFoto,
            @AuthenticationPrincipal Usuario usuario
            ) {
    	
        try {

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
                novoPedido.setFoto(arquivoFoto.getBytes());
            }
            novoPedido.setDataInicioManutencao(LocalDate.now());
            novoPedido.setStatus(0);
            novoPedido.setIdUsuario(usuario.getId());
            novoPedido.setPrioridade(prioridadeService.sugerirPrioridadeInicial(novoPedido));
            pedidoRepository.save(novoPedido);
            return "Pedido salvo com sucesso";
            
        } catch (IOException e) {
            return "Ocorreu um erro ao processar a imagem.";
        }
    }
    
    // Altera o status do pedido
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/alterar-status-pedido/{id}")
    public String alterarStatusPedido(@PathVariable("id") UUID id,
    		@RequestBody Map<String, String> novosValores,
    		@AuthenticationPrincipal Usuario usuario) {
        
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getId().equals(pedido.getIdTecnico())) {
        	return "Você não tem autorização para alterar o status";
        }
        
        int novoStatusId = Integer.parseInt(novosValores.get("novoStatusId"));
        pedido.setStatus(novoStatusId);
        if(novoStatusId == 1) pedido.setObservacoesTecnico(novosValores.get("observacao"));
        
        Notificacao aviso = new Notificacao();
        aviso.setIdUsuario(pedido.getIdUsuario());
        aviso.setStatus(novoStatusId);
        if(novoStatusId == 5) pedido.setDescricao(novosValores.get("descricao"));
        
        
        notificacaoRepository.save(aviso);
        
        pedidoRepository.save(pedido);
        
        return "Status do pedido alterado com sucesso!";
    }
    
    // Retorna as notificações
    @GetMapping
    public List<Notificacao> retornarNotificacoesUsuario(@AuthenticationPrincipal Usuario usuario) {
    	String idUsuario = usuario.getId();
    	return notificacaoRepository.findByIdUsuario(idUsuario);
    }
    
    // Atribui o técino ao pedido
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/atribuir-tecnico/{id}")
    public String atribuirTecnico(@PathVariable("id") UUID id,
    		@RequestBody String idTecnico) {
    	
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	pedido.setIdTecnico(idTecnico);
    	pedidoRepository.save(pedido);
    	
    	return "Tecnico atribuído ao pedido";
    }
    
    // Retorna as O.Ss do equipamento específico
    @GetMapping("/listar-pedidos-equipamento/{id}")
    public List<Pedido> listarManutencoesEquipamento(@PathVariable("id") Long id) {
    	return pedidoRepository.findByIdPC(id);
    }
    
    // Retorna as O.Ss do usuário
    @GetMapping("/listar-pedidos-usuario")
	public List<Pedido> listarManutencoesUsuario (@AuthenticationPrincipal Usuario usuarioLogado) {
		String idUsuario = usuarioLogado.getId();
		return pedidoRepository.findByIdUsuario(idUsuario);
	}
    
    // Retorna as O.Ss atribuídas ao técnico
    @GetMapping("/listar-pedidos-atribuidos")
    public List<Pedido> listarManutencoesAtribuidas(@AuthenticationPrincipal Usuario usuarioLogado) {
    	return pedidoRepository.findByIdUsuario(usuarioLogado.getId());
    }
    
    // Retorna as O.Ss do admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/listar-pedidos")
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoRepository.findAll();
    }

}
