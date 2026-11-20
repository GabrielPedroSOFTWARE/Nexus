package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
	List<Usuario> findByStatus(String status);
    UserDetails findByCpf(String cpf);
    List<Usuario> findByCpfNot(String cpf);
    long countByPerfilNot(PerfilUsuario perfil);
    List<Usuario> findByPerfilAndStatus(PerfilUsuario perfil, String string);
    Optional<Usuario> findByTokenRecuperacao(String tokenRecuperacao);
    Optional<Usuario> findByCodigoEspera(String codigoEspera);
}