package com.example.demo.controller;

import java.io.IOException;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<String> criarNovoPedido(
            @ModelAttribute Pedido novoPedido, 
            @RequestParam(value = "arquivoImagem", required = false) MultipartFile arquivoFoto,
            @AuthenticationPrincipal Usuario usuario
            ) {
        try {
            pedidoService.criarNovoPedido(novoPedido, arquivoFoto, usuario);
            return ResponseEntity.ok("Pedido salvo com sucesso");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Ocorreu um erro ao processar a imagem");
        }
    }
    
    
    // Altera uma O.S
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/admin/alterar-pedido/{id}")
    public ResponseEntity<String> alterarPedido(
    		@ModelAttribute Pedido novoPedido, 
            @RequestParam(value = "arquivoImagem", required = false) MultipartFile arquivoFoto,
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable("id") UUID id
            ) {
    	try {
    		pedidoService.alterarPedido(novoPedido, arquivoFoto, id, usuario);
    		return ResponseEntity.ok("Pedido alterado com sucesso");
    	} catch (IOException e) {
    		return ResponseEntity.badRequest().body("Ocorreu um erro ao processar a imagem");
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
    
    
    // Altera o status da O.S
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
    
    
    // Define a justificativa da O.S
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/admin/definir-justificativa-pedido/{id}")
    public ResponseEntity<String> definirJustificativa(
    		@PathVariable("id") UUID id,
    		@RequestBody String justificativa,
    		@AuthenticationPrincipal Usuario usuario) {
    	
    	try {
    		pedidoService.definirJustificativa(id, justificativa, usuario);
    		return ResponseEntity.ok("Justificativa da O.S alterada");
    		
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    
    // Define o prazo da O.S
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/admin/definir-prazo-pedido/{id}")
    public ResponseEntity<String> definirPrazo(
    		@PathVariable("id") UUID id,
    		@RequestBody LocalDate prazo,
    		@AuthenticationPrincipal Usuario usuario) {
    	
    	try {
    		pedidoService.definirPrazo(id, prazo, usuario);
    		return ResponseEntity.ok("Prazo da O.S definido");
    		
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    // Atribui o técino ao pedido
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/atribuir-tecnico/{id}")
    public ResponseEntity<String> atribuirTecnico(@PathVariable("id") UUID id,
    		@RequestBody String idTecnico, @AuthenticationPrincipal Usuario usuario) {
    	try {
    		pedidoService.atribuirTecnico(id, idTecnico, usuario);
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
    public List<Pedido> listarManutencoesAtribuidasTecnico(
    		@AuthenticationPrincipal Usuario usuarioLogado) {
    	return pedidoService.listarManutencoesAtribuidas(usuarioLogado);
    }
    
    
    // Retorna as O.Ss por frequência
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/listar-pedidos-frequencia")
    public List<Pedido> listarManutencoesPorSequencia(@RequestBody int frequencia) {
    	return pedidoService.listarManutencoesPorFrequencia(frequencia);
    }
    
    // Retorna as O.Ss do admin
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/listar-pedidos")
    public List<Pedido> listarManutencoesAdmin () {
    	return pedidoService.listarManutencoesAdmin();
    }

}
