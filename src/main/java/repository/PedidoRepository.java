package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long>{}