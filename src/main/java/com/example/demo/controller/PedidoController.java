package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.service.PedidoService;

@RestController
public class PedidoController {
	
	@Autowired
	private PedidoService pedidoService;

	// Salva a solicitação no banco de dados
    @PostMapping(value = "/criar-pedido", consumes = "multipart/form-data")
    public String criarNovoPedido(
            @ModelAttribute Pedido novoPedido, 
            @RequestParam(value = "arquivoImagem", required = false) MultipartFile arquivoFoto,
            @AuthenticationPrincipal Usuario usuario
            ) {
        try {
            pedidoService.criarNovoPedido(novoPedido, arquivoFoto, usuario);
            return "Pedido salvo com sucesso";
        } catch (IOException e) {
            return "Ocorreu um erro ao processar a imagem.";
        }
    }
    
    // Cancela a O.S
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/admin/cancelar-pedido/{id}")
    public ResponseEntity<String> cancelarPedido(@PathVariable("id") UUID id,
    		@AuthenticationPrincipal Usuario usuario,
    		@RequestBody String justificativa) {
    	try {
            pedidoService.cancelarPedido(id, usuario, justificativa);
            return ResponseEntity.ok("Pedido cancelado com sucesso");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // Altera o status do pedido
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/admin/alterar-status-pedido/{id}")
    public ResponseEntity<String> alterarStatusPedido(@PathVariable("id") UUID id,
    		@RequestBody Map<String, String> dados,
    		@AuthenticationPrincipal Usuario usuario) {
        try {
        	pedidoService.alterarStatusPedido(id, dados, usuario);
        	return ResponseEntity.ok("Status alterado com sucesso!");
        } catch (RuntimeException e) {
        	return ResponseEntity.badRequest().body(e.getMessage());
        } 
    }
    
    // Atribui o técino ao pedido
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/atribuir-tecnico/{id}")
    public ResponseEntity<String> atribuirTecnico(@PathVariable("id") UUID id,
    		@RequestBody String idTecnico) {
    	try {
    		pedidoService.atribuirTecnico(id, idTecnico);
    		return ResponseEntity.ok("Tecnico atribuído ao pedido");
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    // Retorna as O.Ss do equipamento específico
    @GetMapping("/listar-pedidos-equipamento/{id}")
    public List<Pedido> listarManutencoesEquipamento(@PathVariable("id") Long id) {
    	return pedidoService.listarManutencoesEquipamento(id);
    }
    
    // Retorna as O.Ss do usuário
    @GetMapping("/listar-pedidos-usuario")
	public List<Pedido> listarManutencoesUsuario (@AuthenticationPrincipal Usuario usuarioLogado) {
		return pedidoService.listarManutencoesUsuario(usuarioLogado);
	}
    
    // Retorna as O.Ss atribuídas ao técnico
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/listar-pedidos-atribuidos")
    public List<Pedido> listarManutencoesAtribuidas(@AuthenticationPrincipal Usuario usuarioLogado) {
    	return pedidoService.listarManutencoesAtribuidas(usuarioLogado);
    }
    
    // Retorna as O.Ss do admin
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/listar-pedidos")
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoService.listarManutencoesAdmin();
    }

}
