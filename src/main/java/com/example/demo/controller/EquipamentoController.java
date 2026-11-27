package com.example.demo.controller;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Usuario;
import com.example.demo.service.EquipamentoService;

@RestController
public class EquipamentoController {
	
	@Autowired
    private EquipamentoService equipamentoService;

	
	// Realiza a verificação das peças do computador
    @GetMapping("/user/pecas")
    public Equipamento verificarPecas(@AuthenticationPrincipal Usuario usuarioLogado) {
        return equipamentoService.gerarRelatorioCompleto(usuarioLogado);
    }
	
	
	// Salva o equipamento no banco de dados
    @PostMapping("/user/adicionar-equipamento")
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
    @PutMapping("/user/alterar-equipamento/{id}")
    public ResponseEntity<String> alterarEquipamento(@PathVariable("id") Long id,
    		@RequestBody Equipamento novoEquipamento) {
    	
    	equipamentoService.alterarEquipamento(id, novoEquipamento);
    	return ResponseEntity.ok("Equipamento alterado");
    }
    
    
    // Remove o equipamento pela id
    @DeleteMapping("/user/remover-equipamento/{id}")
    public ResponseEntity<String> removerEquipamento(@PathVariable("id") Long idEquipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado,
    		@RequestBody(required = false) String motivo) {
    	try {
    		equipamentoService.removerEquipamento(idEquipamento, usuarioLogado, motivo);
    		return ResponseEntity.ok("Peça deletada");
    		
    	} catch (RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    }
    
    // Retorna os equipamentos por usuário
    @GetMapping("/adm/equipamentos-por-usuario")
    public List<Equipamento> retornarEquipamentosPorUsuario(@RequestParam("cpfUsuario") String cpfUsuario) {
    	System.out.println("metodo funcioonando");
    	return equipamentoService.retornarEquipamentosPorUsuario(cpfUsuario);
    }
    
    // Retorna os equipamentos que podem receber uma O.S
    @GetMapping("/user/equipamentos-para-pedidos")
    public List<Equipamento> retornarEquipamentosParaPedidos(
    	@RequestBody String cpfUsuario) {
        return equipamentoService.retornarEquipamentosParaPedidos(cpfUsuario);
    }
    
    
    // Retorna os equipamentos cadastrados
	@GetMapping("/user/listar-equipamentos-usuario")
	public List<Equipamento> listarEquipamentosUsuario(@AuthenticationPrincipal Usuario usuarioLogado) {
	    return equipamentoService.listarEquipamentosUsuario(usuarioLogado);
	}
	
	
	// Retorna os dados brutos para as estatísticas
    @GetMapping("/backoffice/totalUsuarios")
    public Map<String, Long> retornarDadosEstatisticas() {
    	return equipamentoService.retornarDadosEstatisticas();
    }
    
    
    // Verifica se há alguma O.S aberta
    @GetMapping("/existe-pedido-em-aberto/{idPC}")
    public ResponseEntity<?> verificarPedidoAberto(@PathVariable("idPC") Long idPC) {
    	boolean pedidoEmAberto = equipamentoService.existePedidoEmAberto(idPC);
    	return ResponseEntity.ok(Map.of("pedidoEmAberto", pedidoEmAberto));
    }
    
    
    // Retorna última manutenção
    @GetMapping("/retornar-ultima-manutencao/{idPC}")
    public ResponseEntity<?> retornarUltimaManutencao(@PathVariable("idPC") Long idPC) {
    	String ultimaManutencao = equipamentoService.retornarUltimaManutencao(idPC);
    	return ResponseEntity.ok(Map.of("ultimaManutencao", ultimaManutencao));
    }
}