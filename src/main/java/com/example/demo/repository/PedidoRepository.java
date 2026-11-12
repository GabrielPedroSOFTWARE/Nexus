package com.example.demo.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, UUID>{
	List<Pedido> findByIdUsuario(String id);
	List<Pedido> findByIdTecnico(String id);
	List<Pedido> findByIdPC(Long idPC);
	List<Pedido> findByFrequencia(Integer frequencia);
	long countByIdPCAndDataInicioManutencaoAfter(Long idPC, LocalDate data);
}