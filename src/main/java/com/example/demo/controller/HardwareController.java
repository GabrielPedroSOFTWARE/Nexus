package com.example.demo.controller;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.HardwareService;

@RestController
public class HardwareController {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
    private HardwareService hardwareService;
	@Autowired
    private EquipamentoRepository equipamentoRepository;
	@Autowired
	private PedidoRepository pedidoRepository;

	// Salva o equipamento no banco de dados
    @PostMapping("/adicionar-equipamento")
    public String adicionarNovoEquipamento(
    		@RequestBody Equipamento equipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado) {
        
        equipamento.setIdUsuario(usuarioLogado.getId());
        equipamento.setPedidos(new ArrayList<Pedido>());
        
    	equipamentoRepository.save(equipamento);
        return "Peça salva com sucesso no banco de dados!";
    }
    
    // Remove o equipamento pela id
    @DeleteMapping("/remover-equipamento/{id}")
    public String removerEquipamento(@PathVariable("id") UUID id) {
    	equipamentoRepository.deleteById(id);
    	return "Peça deletada";
    }
    
    // Salva a solicitação no banco de dados
    @PostMapping(value = "/criar-pedido", consumes = "multipart/form-data")
    public String criarNovoPedido(
            @ModelAttribute Pedido novoPedido, 
            @RequestParam(value = "arquivoImagem", required = false) MultipartFile arquivoFoto
            ) {
    	
        try {

            if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
                novoPedido.setFoto(arquivoFoto.getBytes());
            }
            
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
    		@RequestBody int novoStatusId) {
        
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        pedido.setStatus(novoStatusId);
        
        pedidoRepository.save(pedido);
        
        return "Status do pedido alterado com sucesso!";
    }
    
    // Atribui o técino ao pedido
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/atribuir-tecnico/{id}")
    public String atribuirTecnico(@PathVariable("id") UUID id,
    		@RequestBody String tecnico) {
    	
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	pedido.setTecnico(tecnico);
    	pedidoRepository.save(pedido);
    	
    	return "Tecnico atribuído ao pedido";
    }
    
    // Realiza a verificação das peças do computador
    @GetMapping("/pecas")
    public Equipamento verificarPecas(@AuthenticationPrincipal Usuario usuarioLogado) {
        return hardwareService.gerarRelatorioCompleto(usuarioLogado);
    }
    
    // Retorna os equipamentos cadastrados
	@GetMapping("/listar-equipamentos-usuario")
	public List<Equipamento> listarEquipamentosUsuario(@AuthenticationPrincipal Usuario usuarioLogado) {
		UUID idUsuario = usuarioLogado.getId();
	    return equipamentoRepository.findByIdUsuario(idUsuario);
	}
	
	// Retorna os dados brutos para as estatísticas
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/totalUsuarios")
    public Map<String, Long> retornarDadosEstatisticas(@AuthenticationPrincipal Usuario usuarioLogado) {
    	Map<String, Long> dados = new HashMap<String, Long>();
    	dados.put("totalUsuarios", usuarioRepository.count());
    	dados.put("totalMaquinas", equipamentoRepository.count());
    	return dados;
    }
    
    // Retorna as O.Ss do equipamento específico
    @GetMapping("/listar-pedidos-equipamento/{id}")
    public List<Pedido> listarManutencoesEquipamento(@PathVariable("id") UUID id) {
    	return pedidoRepository.findByIdPC(id);
    }
    
    // Retorna as O.Ss do usuário
    @GetMapping("/listar-pedidos-usuario")
	public List<Pedido> listarManutencoesUsuario (@AuthenticationPrincipal Usuario usuarioLogado) {
		UUID idUsuario = usuarioLogado.getId();
		return pedidoRepository.findByIdUsuario(idUsuario);
	}
    
    // Retorna as O.Ss do admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/listar-pedidos")
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoRepository.findAll();
    }
    
}
