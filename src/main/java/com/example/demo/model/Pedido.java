package com.example.demo.model;
import java.time.LocalDate;
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
	private Long idPC;
	private String idUsuario;
	private String apelido;
	private Integer status;
	private Integer prioridade;
	private String problema;
	private LocalDate dataInicioManutencao;
	private LocalDate dataFinalManutencao;
	private String idTecnico;
	@Lob
    private byte[] foto;
	private String descricao;
	private String observacoesTecnico;
}
