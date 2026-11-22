package com.example.demo.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.HistoricoUsuario;

public interface HistoricoUsuarioRepository extends JpaRepository<HistoricoUsuario, UUID>{}