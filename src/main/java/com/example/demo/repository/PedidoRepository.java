package com.example.demo.repository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, UUID>{
	List<Pedido> findByIdUsuario(String id);
	List<Pedido> findByIdPC(UUID idPC);
}