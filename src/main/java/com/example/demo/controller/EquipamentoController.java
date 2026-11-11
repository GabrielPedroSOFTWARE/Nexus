package com.example.demo.controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.EquipamentoService;

@RestController
public class EquipamentoController {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
    private EquipamentoService equipamentoService;
	@Autowired
    private EquipamentoRepository equipamentoRepository;

	// Salva o equipamento no banco de dados
    @PostMapping("/adicionar-equipamento")
    public String adicionarNovoEquipamento(
    		@RequestBody Equipamento equipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado) {
        
        equipamento.setIdUsuario(usuarioLogado.getId());

        
    	equipamentoRepository.save(equipamento);
        return "Peça salva com sucesso no banco de dados!";
    }
    
    // Remove o equipamento pela id
    @DeleteMapping("/remover-equipamento/{id}")
    public String removerEquipamento(@PathVariable("id") Long idEquipamento,
    		@AuthenticationPrincipal Usuario usuarioLogado) {
    	String idUsuario = usuarioLogado.getId();
    	boolean fazParte = equipamentoRepository
    			.verificarPossivelExclusaoEquipamento(idEquipamento, idUsuario);
    	if(!fazParte) return "Há uma O.S em aberto para esse equipamento";
    	equipamentoRepository.deleteById(idEquipamento);
    	return "Peça deletada";
    }
    
    @GetMapping("/equipamentos-para-pedidos")
    public List<Equipamento> retornarEquipamentosParaPedidos(
    		@PathVariable("idUsuario") String idUsuario) {
        return equipamentoRepository.buscarEquipamentosComStatusFinalizadoPorUsuario(idUsuario);
    }
    
    // Realiza a verificação das peças do computador
    @GetMapping("/pecas")
    public Equipamento verificarPecas(@AuthenticationPrincipal Usuario usuarioLogado) {
        return equipamentoService.gerarRelatorioCompleto(usuarioLogado);
    }
    
    // Retorna os equipamentos cadastrados
	@GetMapping("/listar-equipamentos-usuario")
	public List<Equipamento> listarEquipamentosUsuario(@AuthenticationPrincipal Usuario usuarioLogado) {
		String idUsuario = usuarioLogado.getId();
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
    
}
