package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Ou UUID, como preferir
    private UUID id;
    
    private String idUsuario; // Para quem é o aviso
    private Integer status;
    private String resumo;
    private LocalDate prazo;
    
    private boolean lida = false; 
}