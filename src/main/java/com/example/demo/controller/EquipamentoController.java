package com.example.demo.controller;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Usuario;
import com.example.demo.service.EquipamentoService;

@RestController
public class EquipamentoController {
	
	@Autowired
    private EquipamentoService equipamentoService;

	
	// Salva o equipamento no banco de dados
    @PostMapping("/adicionar-equipamento")
    public ResponseEntity<String> adicionarNovoEquipamento(
    		@RequestBody Equipamento equipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado) {
    	try {
    		equipamentoService.adicionarNovoEquipamento(equipamento, usuarioLogado);
    		return ResponseEntity.ok("Peça salva com sucesso no banco de dados!");    		
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    
    // Atualiza o equipamento no banco de dados
    @PutMapping("/alterar-equipamento/{id}")
    public ResponseEntity<String> alterarEquipamento(@PathVariable("id") Long id,
    		@RequestBody Equipamento novoEquipamento) {
    	
    	equipamentoService.alterarEquipamento(id, novoEquipamento);
    	return ResponseEntity.ok("Equipamento alterado");
    }
    
    
    // Remove o equipamento pela id
    @DeleteMapping("/remover-equipamento/{id}")
    public ResponseEntity<String> removerEquipamento(@PathVariable("id") Long idEquipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado) {
    	try {
    		
    		equipamentoService.removerEquipamento(idEquipamento, usuarioLogado);
    		return ResponseEntity.ok("Peça deletada");
    		
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    
    @GetMapping("/equipamentos-para-pedidos")
    public List<Equipamento> retornarEquipamentosParaPedidos(
    	@PathVariable("idUsuario") String idUsuario) {
        return equipamentoService.retornarEquipamentosParaPedidos(idUsuario);
    }
    
    
    // Realiza a verificação das peças do computador
    @GetMapping("/pecas")
    public Equipamento verificarPecas(@AuthenticationPrincipal Usuario usuarioLogado) {
        return equipamentoService.gerarRelatorioCompleto(usuarioLogado);
    }
    
    
    // Retorna os equipamentos cadastrados
	@GetMapping("/listar-equipamentos-usuario")
	public List<Equipamento> listarEquipamentosUsuario(@AuthenticationPrincipal Usuario usuarioLogado) {
	    return equipamentoService.listarEquipamentosUsuario(usuarioLogado);
	}
	
	
	// Retorna os dados brutos para as estatísticas
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/totalUsuarios")
    public Map<String, Long> retornarDadosEstatisticas() {
    	return equipamentoService.retornarDadosEstatisticas();
    }
    
}
