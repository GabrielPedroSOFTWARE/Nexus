package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    private String nome;
    private String email;
    private String setor;
    private String telefone;
    private LocalDateTime dataHoraCadastro;
    @Id
    @Column(unique = true)
    private String cpf;
    private String senha;
    private String justificativa;

    @Enumerated(EnumType.STRING)
    private PerfilUsuario perfil = PerfilUsuario.PENDENT;
    private String status = "Aguardando análise";
    private boolean ehAdmin = false;
    private Integer versaoToken = 1;
    
    private String tokenRecuperacao;
    private String codigoEspera;
    private boolean recuperacaoConfirmada = false;
    private LocalDateTime validadeTokenRecuperacao;

 // Define a autoridade do usuário baseado no perfil
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
    	SimpleGrantedAuthority admin = (this.ehAdmin) ?
    			new SimpleGrantedAuthority("ROLE_ADMIN") : null;
    	SimpleGrantedAuthority perfilUsuario = switch(this.perfil) {
    		case PerfilUsuario.USER -> new SimpleGrantedAuthority("ROLE_USER");
    		case PerfilUsuario.SUPPORT_MANAGER -> new SimpleGrantedAuthority("ROLE_SUPPORT_MANAGER");
		default -> new SimpleGrantedAuthority("ROLE_PENDENT"); 
    	};
    	return Stream.of(admin, perfilUsuario)
    			.filter(Objects::nonNull)
    			.toList();
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return senha;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return cpf;
    }
    
}