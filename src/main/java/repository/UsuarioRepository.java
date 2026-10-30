package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	// Procura o usuário na base de dados pelo login
    UserDetails findByLogin(String login); 
}