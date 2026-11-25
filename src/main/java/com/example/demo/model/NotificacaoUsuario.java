package com.example.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NotificacaoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String cpfUsuario;
    private LocalDateTime dataHoraCriacao = LocalDateTime.now();
    @Transient
    private String cpfUsuarioDescriptografado;
    private Integer status;
    private String descrição;
    private LocalDate prazo;
    
    private boolean lida = false; 
}