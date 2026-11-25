package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Pedido;
import com.example.demo.model.PedidoDetalhesDTO;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;

class PedidoServiceOperationsTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EquipamentoRepository equipamentos = mock(EquipamentoRepository.class);
    private final NotificacaoUsuarioService notificacoesUsuario = mock(NotificacaoUsuarioService.class);
    private final NotificacaoBackOfficeService notificacoesBackOffice = mock(NotificacaoBackOfficeService.class);
    private final HistoricoPedidoService historico = mock(HistoricoPedidoService.class);
    private final CriptografiaCpfService criptografia = mock(CriptografiaCpfService.class);
    private final PedidoService service = new PedidoService();

    @BeforeEach
    void prepararDependencias() {
        ReflectionTestUtils.setField(service, "pedidoRepository", pedidos);
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarios);
        ReflectionTestUtils.setField(service, "equipamentoRepository", equipamentos);
        ReflectionTestUtils.setField(service, "notificacaoUsuarioService", notificacoesUsuario);
        ReflectionTestUtils.setField(service, "notificacaoBackOfficeService", notificacoesBackOffice);
        ReflectionTestUtils.setField(service, "historicoPedidoService", historico);
        ReflectionTestUtils.setField(service, "criptografiaService", criptografia);
    }

    @Test
    void devolucaoFilaPersistePedidoEJustificativaNoHistorico() {
        Pedido pedido = pedidoAtivo(2);
        Usuario tecnico = usuario("cpf-tecnico", false);
        when(pedidos.findById("OS-2026-00000001")).thenReturn(Optional.of(pedido));

        service.devolverParaAFila(pedido.getId(), tecnico, "Peça incompatível");

        assertEquals(0, pedido.getStatus());
        assertNull(pedido.getCpfTecnico());
        verify(pedidos).save(pedido);
        verify(historico).registrarAcao(eq(pedido), eq("cpf-tecnico"), contains("Peça incompatível"));
    }

    @Test
    void devolucaoFilaRejeitaJustificativaVaziaSemPersistir() {
        Pedido pedido = pedidoAtivo(2);
        when(pedidos.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThrows(RuntimeException.class,
                () -> service.devolverParaAFila(pedido.getId(), usuario("cpf-tecnico", false), "  "));

        verify(pedidos, never()).save(any(Pedido.class));
    }

    @Test
    void ordemAtrasadaNaoMudaStatusSemNovoPrazoEJustificativa() {
        Pedido pedido = pedidoAtivo(2);
        pedido.setDataFinalManutencao(LocalDate.now().minusDays(1));
        when(pedidos.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        assertThrows(RuntimeException.class,
                () -> service.alterarStatusPedido(pedido.getId(), Map.of("novoStatusId", "3"),
                        usuario("cpf-tecnico", false)));

        verify(pedidos, never()).save(any(Pedido.class));
    }

    @Test
    void qualquerAlteracaoDePrazoExigeJustificativa() {
        Pedido pedido = pedidoAtivo(2);
        pedido.setDataFinalManutencao(LocalDate.now().plusDays(2));
        when(pedidos.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        String novoPrazo = LocalDate.now().plusDays(4).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        assertThrows(RuntimeException.class,
                () -> service.alterarStatusPedido(pedido.getId(), Map.of(
                        "novoStatusId", "2", "prazo", novoPrazo), usuario("cpf-tecnico", false)));

        verify(pedidos, never()).save(any(Pedido.class));
    }

    @Test
    void alteracaoPrazoAtrasadoSalvaPrazoJustificativaEHistorico() {
        Pedido pedido = pedidoAtivo(2);
        pedido.setDataFinalManutencao(LocalDate.now().minusDays(1));
        when(pedidos.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        String novoPrazo = LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String justificativa = "Aguardando disponibilidade da peça";

        service.alterarStatusPedido(pedido.getId(), Map.of(
                "novoStatusId", "3", "prazo", novoPrazo, "justificativa", justificativa),
                usuario("cpf-tecnico", false));

        assertEquals(3, pedido.getStatus());
        assertEquals(LocalDate.now().plusDays(3), pedido.getDataFinalManutencao());
        assertFalse(pedido.isAlertaPrazoEnviado());
        verify(pedidos).save(pedido);
        verify(historico).registrarAcao(eq(pedido), eq("cpf-tecnico"), contains(justificativa));
    }

    @Test
    void detalhesRetornamSolicitanteMascaradoEEquipamento() {
        Pedido pedido = pedidoAtivo(2);
        pedido.setIdPC(42L);
        when(pedidos.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        Usuario solicitante = usuario("cpf-cifrado", false);
        solicitante.setNome("João da Silva");
        solicitante.setSetor("TI");
        solicitante.setTelefone("61999998888");
        when(usuarios.findByCpf("cpf-usuario")).thenReturn((UserDetails) solicitante);
        when(criptografia.descriptografarEMascarar("cpf-cifrado")).thenReturn("***.***.***-00");
        Equipamento equipamento = new Equipamento();
        equipamento.setId(42L);
        equipamento.setNumeroSerie("NEX-0042");
        equipamento.setModelo("ThinkCentre");
        equipamento.setEhNotebook(false);
        equipamento.setDiscos(List.of("SSD 512 GB"));
        when(equipamentos.findById(42L)).thenReturn(Optional.of(equipamento));

        PedidoDetalhesDTO detalhes = service.obterDetalhesPedido(pedido.getId());

        assertNotNull(detalhes.solicitante());
        assertEquals("João da Silva", detalhes.solicitante().nome());
        assertEquals("***.***.***-00", detalhes.solicitante().cpfExibicao());
        assertEquals("ThinkCentre", detalhes.equipamento().modelo());
        assertEquals("NEX-0042", detalhes.equipamento().numeroSerie());
        assertFalse(detalhes.equipamento().ehNotebook());
        assertTrue(detalhes.equipamento().discos().contains("SSD 512 GB"));
    }

    private Pedido pedidoAtivo(int status) {
        Pedido pedido = new Pedido();
        pedido.setId("OS-2026-00000001");
        pedido.setCpfUsuario("cpf-usuario");
        pedido.setCpfTecnico("cpf-tecnico");
        pedido.setStatus(status);
        return pedido;
    }

    private Usuario usuario(String cpf, boolean admin) {
        Usuario usuario = new Usuario();
        usuario.setCpf(cpf);
        usuario.setEhAdmin(admin);
        return usuario;
    }
}
