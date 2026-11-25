package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.NotificacaoUsuario;


public interface NotificacaoUsuarioRepository extends JpaRepository<NotificacaoUsuario, UUID> {
	List<NotificacaoUsuario> findByCpfUsuarioOrderByDataHoraCriacaoDesc(String cpfUsuario);
}
