package com.example.demo.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Equipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
	List<Equipamento> findByIdUsuario(String idUsuario);
	
	@Query("SELECT e FROM Equipamento e WHERE e.idUsuario = :idUsuario AND e.id IN "
	         + "(SELECT p.idPC FROM Pedido p WHERE p.status NOT IN (5, 6))")
    List<Equipamento> buscarEquipamentosComStatusFinalizadoPorUsuario(@Param("idUsuario") String idUsuario);
	
	@Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
		       "FROM Equipamento e " +
		       "WHERE e.id = :idEquipamento " +
		       "AND e.idUsuario = :idUsuario " +
		       "AND NOT EXISTS (" +
		       "    SELECT 1 FROM Pedido p " +
		       "    WHERE p.idPC = e.id AND p.status NOT IN (5, 6)" +
		       ")")
	    boolean verificarPossivelExclusaoEquipamento(
	            @Param("idEquipamento") Long idEquipamento, 
	            @Param("idUsuario") String idUsuario);
}