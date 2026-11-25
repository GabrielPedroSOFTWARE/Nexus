package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.model.HistoricoPedido;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.HistoricoPedidoRepository;
import com.example.demo.repository.PedidoRepository;

@SpringBootTest
@ActiveProfiles("test")
class PedidoServicePersistenceIntegrationTest {

    @Autowired
    private PedidoService service;

    @Autowired
    private PedidoRepository pedidos;

    @Autowired
    private HistoricoPedidoRepository historicos;

    @Test
    void devolucaoFilaPersistePedidoEHistoricoNoBancoDeTeste() {
        Pedido pedido = novoPedido(2);
        pedido.setDataFinalManutencao(LocalDate.now().plusDays(4));
        pedidos.save(pedido);
        Usuario tecnico = usuario("cpf-tecnico");

        service.devolverParaAFila(pedido.getId(), tecnico, "Peça incompatível");

        Pedido salvo = pedidos.findById(pedido.getId()).orElseThrow();
        assertEquals(0, salvo.getStatus());
        assertNull(salvo.getCpfTecnico());
        assertNull(salvo.getDataFinalManutencao());
        List<HistoricoPedido> historico = historicos.findByIdPedidoOrderByDataHoraDesc(pedido.getId());
        assertEquals(1, historico.size());
        assertTrue(historico.get(0).getAlteracao().contains("Peça incompatível"));
    }

    @Test
    void prazoNovoEJustificativaPersistemAoAtualizarUmaOrdemAtrasada() {
        Pedido pedido = novoPedido(2);
        pedido.setDataFinalManutencao(LocalDate.now().minusDays(1));
        pedidos.save(pedido);
        String justificativa = "Aguardando disponibilidade da peça";
        LocalDate novoPrazo = LocalDate.now().plusDays(5);
        String prazoTexto = novoPrazo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        service.alterarStatusPedido(pedido.getId(), Map.of(
                "novoStatusId", "3",
                "prazo", prazoTexto,
                "justificativa", justificativa), usuario("cpf-tecnico"));

        Pedido salvo = pedidos.findById(pedido.getId()).orElseThrow();
        assertEquals(3, salvo.getStatus());
        assertEquals(novoPrazo, salvo.getDataFinalManutencao());
        List<HistoricoPedido> historico = historicos.findByIdPedidoOrderByDataHoraDesc(pedido.getId());
        assertEquals(1, historico.size());
        assertTrue(historico.get(0).getAlteracao().contains(justificativa));
    }

    private Pedido novoPedido(int status) {
        Pedido pedido = new Pedido();
        pedido.setId("OS-TEST-" + UUID.randomUUID().toString().substring(0, 8));
        pedido.setIdPC(1L);
        pedido.setCpfUsuario("cpf-usuario");
        pedido.setCpfTecnico("cpf-tecnico");
        pedido.setStatus(status);
        pedido.setDataInicioManutencao(LocalDate.now());
        pedido.setProblema("Problema de teste");
        pedido.setFrequencia(1);
        return pedido;
    }

    private Usuario usuario(String cpf) {
        Usuario usuario = new Usuario();
        usuario.setCpf(cpf);
        return usuario;
    }
}
