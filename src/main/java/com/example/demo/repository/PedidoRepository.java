package com.example.demo.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, UUID>{
	List<Pedido> findByCpfUsuario(String cpf);
	
	List<Pedido> findByCpfTecnicoAndStatusNotIn(String cpfTecnico, List<Integer> status);
	
	List<Pedido> findByIdPC(Long idPC);
	
	List<Pedido> findByFrequencia(Integer frequencia);
	
	long countByIdPCAndDataInicioManutencaoAfter(Long idPC, LocalDate data);
	
	List<Pedido> findByStatusNotInAndAlertaPrazoEnviadoFalseAndDataFinalManutencaoBetween(
	        List<Integer> statusIgnorados,
	        LocalDate dataInicio,
	        LocalDate dataFim
	);
}