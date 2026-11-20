package com.example.demo.model;

import java.time.LocalDateTime;
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
public class NotificacaoTecnico {
	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
	private String cpfTecnico;
	private UUID idPedido;
	private String mensagem;
	private boolean lida = false;
	private LocalDateTime dataCriacao = LocalDateTime.now();
}
