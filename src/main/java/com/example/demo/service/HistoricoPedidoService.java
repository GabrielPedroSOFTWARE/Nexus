package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.HistoricoPedido;
import com.example.demo.model.Pedido;
import com.example.demo.repository.HistoricoPedidoRepository;

@Service
public class HistoricoPedidoService {

    @Autowired
    private HistoricoPedidoRepository historicoRepository;

    
    // Registra o histórico da alteração do pedido
    public void registrarAcao(Pedido pedido, String idUsuario) {
        HistoricoPedido historico = new HistoricoPedido();
        
        historico.setIdPedido(pedido.getId());
        historico.setIdUsuarioAutor(idUsuario);
        historico.setDataHora(LocalDateTime.now());
        historico.setApelido(pedido.getApelido());
        historico.setStatus(pedido.getStatus());
        historico.setPrioridade(pedido.getPrioridade());
        historico.setFrequencia(pedido.getFrequencia());
        historico.setProblema(pedido.getProblema());
        historico.setDataInicioManutencao(pedido.getDataInicioManutencao());
        historico.setDataFinalManutencao(pedido.getDataFinalManutencao());
        historico.setIdTecnico(pedido.getIdTecnico());
        historico.setDescricao(pedido.getDescricao());
        historico.setObservacoesTecnico(pedido.getObservacoesTecnico());
        historico.setJustificativaCancelamento(pedido.getJustificativaCancelamento());
        historicoRepository.save(historico);
    }
    
    // Retorna todo o histórico de alteração de um pedido
    public List<HistoricoPedido> buscarHistoricoDoPedido(UUID idPedido) {
        return historicoRepository.findByIdPedidoOrderByDataHoraDesc(idPedido);
    }
}