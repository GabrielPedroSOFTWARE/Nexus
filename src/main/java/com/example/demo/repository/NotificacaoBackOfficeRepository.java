package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.NotificacaoBackOffice;

public interface NotificacaoBackOfficeRepository extends JpaRepository<NotificacaoBackOffice, UUID>{
	
	List<NotificacaoBackOffice> findByCpfTecnicoAndLidaFalseOrderByDataCriacaoDesc(String cpfTecnico);
	NotificacaoBackOffice findByCpfTecnico(String cpfTecnico);

}
