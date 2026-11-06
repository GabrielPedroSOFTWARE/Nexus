package com.example.demo.repository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Equipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {
	List<Equipamento> findByIdUsuario(UUID idUsuario);
}