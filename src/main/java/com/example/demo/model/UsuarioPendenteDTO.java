package com.example.demo.model;

import java.time.LocalDateTime;

public record UsuarioPendenteDTO(String nome, String email, String setor, String telefone,
		LocalDateTime dataHoraCadastro, String cpf, String cpfChave, String justificativa) {}
