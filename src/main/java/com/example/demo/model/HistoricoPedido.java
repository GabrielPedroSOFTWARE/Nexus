package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoPedido {
	@Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String idPedido;
    private String cpfUsuarioAutor;
    @Transient
    private String cpfUsuarioAutorDescriptografado;
    private String dataHora; 
    private String apelido;
	private Integer status;
	private Integer prioridade;
	private Integer frequencia;
	private String problema;
	private LocalDate dataInicioManutencao;
	private LocalDate dataFinalManutencao;
	private String idTecnico;
	private String descricao;
	private String observacoesTecnico;
	private String justificativaCancelamento;
	@Column(length = 1024)
	private String alteracao;
}
