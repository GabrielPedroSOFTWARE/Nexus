package com.example.demo.model;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
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
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_seq")
	@SequenceGenerator(
	    name = "pedido_seq",
	    sequenceName = "pedido_sequence",
	    initialValue = 100000,
	    allocationSize = 1
	)
	private Long id;
	private String numeroSerie;
	private String cpfUsuario;
	@Transient
	private String cpfUsuarioDescriptografado;
	private String fabricante;
	private String modelo;
	private String processador;
	private Integer ram;
	@ElementCollection
	private List<String> placasDeVideo;
	@ElementCollection
	private List<String> discos;
	private String sistemaOperacional;
	private boolean ehNotebook;
	
	
	// Atualiza o Hardware
	public Equipamento atualizarHardware(Equipamento equipamento, String cpf) {
	    this.cpfUsuario = cpf;
	    this.ram = equipamento.getRam();
	    this.discos = equipamento.getDiscos();
	    this.placasDeVideo = equipamento.getPlacasDeVideo();
	    return this;
	}
	
}
