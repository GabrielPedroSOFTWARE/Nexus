package com.example.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.HistoricoPedido;
import com.example.demo.service.HistoricoPedidoService;

@RestController
public class HistoricoPedidoController {
	
	@Autowired
	HistoricoPedidoService historicoPedidoService;

	// Retorna o histórico das O.Ss alteradas
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPPORT')")
	@GetMapping("/retornar-historico-pedido-alt/{id}")
	public List<HistoricoPedido> retornarHistoricoAlteracaoPedido(@PathVariable("id") UUID id) {
		return historicoPedidoService.buscarHistoricoDoPedido(id);
	}
}
