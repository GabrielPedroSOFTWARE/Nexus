package com.example.demo.model;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	@Column(length = 32) // "OS-" (3) + ano (4) + 8 números = 15 caracteres
	private String id;
	private LocalDateTime dataHoraAbertura;
	private LocalDateTime dataHoraAtribuicaoTecnico;
	private LocalDateTime dataHoraInicioDiagnostico;
	private LocalDateTime dataHoraProntaRetirada;
	private LocalDateTime dataHoraConclusao;
	private Long idPC;
	private String cpfUsuario;
	private String cpfUsuarioDescriptografado;
	private String apelido;
	private Integer status;
	private Integer prioridade;
	private Integer frequencia;
	private String problema;
	private LocalDate dataInicioManutencao;
	private LocalDate dataFinalManutencao;
	private String cpfTecnico;
	private String cpfTecnicoDescriptografado;
	@Lob
    private byte[] foto;
	private String nomeArquivoFoto;
	private String tipoArquivoFoto;
	private String descricao;
	private String observacoesTecnico;
	private String justificativa;
	private String justificativaCancelamento;
	private boolean alertaPrazoEnviado = false;
	private boolean estahAtrasada = false;
	
	private LocalDateTime ultimaAlteracao;
	private int nivelAlertaInatividade = 0;
	
	public void registrarMovimentacao() {
	    this.ultimaAlteracao = LocalDateTime.now();
	    this.nivelAlertaInatividade = 0;
	}
	
	
	// Preenche os dados iniciais de uma O.S
	public void definirDadosIniciais(String cpfUsuario, MultipartFile arquivoFoto) 
	        throws IOException {
	    this.cpfUsuario = cpfUsuario;
	    this.dataInicioManutencao = LocalDate.now();
	    this.status = 0;
	    anexarFoto(arquivoFoto);
	}

	// Preserva os dados que não podem ser alterados
	public void preservarDadosOriginais(Pedido original) {
	    this.id = original.getId();
	    this.cpfUsuario = original.getCpfUsuario();
	    this.dataInicioManutencao = original.getDataInicioManutencao();
	}

	// Anexa a nova foto à O.S, caso houver
	public void anexarFoto(MultipartFile arquivoFoto) throws IOException {
	    if (arquivoFoto != null && !arquivoFoto.isEmpty()) {
	        this.foto = arquivoFoto.getBytes();
	        this.nomeArquivoFoto = arquivoFoto.getOriginalFilename();
	        this.tipoArquivoFoto = arquivoFoto.getContentType();
	    }
	}
}
