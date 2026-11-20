package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.NotificacaoTecnico;

public interface NotificacaoTecnicoRepository extends JpaRepository<NotificacaoTecnico, UUID>{
	
	List<NotificacaoTecnico> findByCpfTecnicoAndLidaFalseOrderByDataCriacaoDesc(String cpfTecnico);
	NotificacaoTecnico findByCpfTecnico(String cpfTecnico);

}
