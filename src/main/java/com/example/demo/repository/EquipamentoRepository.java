package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Equipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
	List<Equipamento> findByCpfUsuario(String cpfUsuario);
	
	
	@Query("SELECT e FROM Equipamento e WHERE e.cpfUsuario = :cpfUsuario AND e.id IN "
	         + "(SELECT p.idPC FROM Pedido p WHERE p.status NOT IN (5, 6))")
    List<Equipamento> buscarEquipamentosComStatusFinalizadoPorUsuario(@Param("cpfUsuario") String cpfUsuario);
	
	
	@Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
		       "FROM Equipamento e " +
		       "WHERE e.id = :idEquipamento " +
		       "AND e.cpfUsuario = :cpfUsuario " +
		       "AND NOT EXISTS (" +
		       "    SELECT 1 FROM Pedido p " +
		       "    WHERE p.idPC = e.id AND p.status NOT IN (5, 6)" +
		       ")")
	boolean verificarPossivelExclusaoEquipamento(
			@Param("idEquipamento") Long idEquipamento,
			@Param("cpfUsuario") String cpfUsuario);
	
	
	Equipamento findByNumeroSerie(String numeroSerie);
	
	boolean existsByNumeroSerieAndCpfUsuarioNot(String numeroSerie, String cpfUsuario);
	
	Optional<Equipamento> findByCpfUsuarioAndFabricanteAndModeloAndProcessador(
	        String cpfUsuario, 
	        String fabricante, 
	        String modelo, 
	        String processador
	);
}