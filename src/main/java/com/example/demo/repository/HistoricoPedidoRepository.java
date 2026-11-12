package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.HistoricoPedido;

public interface HistoricoPedidoRepository extends JpaRepository<HistoricoPedido, UUID> {
    List<HistoricoPedido> findByIdPedidoOrderByDataHoraDesc(UUID idPedido);
}