package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Notificacao;


public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
	List<Notificacao> findByIdUsuario(String id);
}
