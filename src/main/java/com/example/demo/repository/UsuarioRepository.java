package com.example.demo.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
	List<Usuario> findByStatus(String status);
	// Procura o usuário na base de dados pelo login
    UserDetails findByLogin(String login); 
}