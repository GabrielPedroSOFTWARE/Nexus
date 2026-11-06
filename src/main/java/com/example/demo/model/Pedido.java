package com.example.demo.model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
	private UUID idPC;
	private UUID idUsuario;
	private String apelido;
	private int status;
	private String problema;
	private LocalDate dataManutencao;
	private LocalTime mttr;
	private String tecnico;
	@Lob
    private byte[] foto;
}
