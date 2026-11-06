package com.example.demo.model;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Classe modelo do equipamento
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipamento {
	
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private UUID idUsuario;
	private String fabricante;
	private String modelo;
	private String processador;
	private int ram;
	@ElementCollection
	private List<Pedido> pedidos;
	private String garantia;
	@ElementCollection
	private List<String> placasDeVideo;
	@ElementCollection
	private List<String> discos;
	
}
