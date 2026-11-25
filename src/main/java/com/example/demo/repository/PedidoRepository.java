package com.example.demo.repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Pedido;


public interface PedidoRepository extends JpaRepository<Pedido, String>{
	List<Pedido> findByCpfUsuario(String cpf);
	
	List<Pedido> findByCpfTecnicoAndStatusNotIn(String cpfTecnico, List<Integer> status);
	
	List<Pedido> findByIdPC(Long idPC);
	
	List<Pedido> findByFrequencia(Integer frequencia);
	
	long countByIdPCAndStatusNotAndDataHoraAberturaAfter(
	        Long idPC,
	        Integer statusCancelado,
	        LocalDateTime dataLimite
	);
	
	List<Pedido> findByStatusNotInAndAlertaPrazoEnviadoFalseAndDataFinalManutencaoBetween(
	        List<Integer> statusIgnorados,
	        LocalDate dataInicio,
	        LocalDate dataFim
	);
	
	@Query("""
		    SELECT p FROM Pedido p
		    WHERE p.status NOT IN :statusIgnorados
		      AND p.cpfTecnico IS NOT NULL
		      AND p.nivelAlertaInatividade = :nivel
		      AND p.ultimaAlteracao <= :limite
		""")
		List<Pedido> buscarPedidosInativos(
		        @Param("statusIgnorados") List<Integer> statusIgnorados,
		        @Param("nivel") int nivel,
		        @Param("limite") LocalDateTime limite
		);
	
	List<Pedido> findByStatusAndDataHoraConclusaoBetween(
	        int status,
	        LocalDateTime inicio,
	        LocalDateTime fim
	);
	boolean existsByIdPCAndStatusNotIn(Long idPC, List<Integer> statusFinalizados);
	Optional<Pedido> findFirstByIdPCAndDataHoraConclusaoIsNotNullOrderByDataHoraConclusaoDesc(Long idPC);
}