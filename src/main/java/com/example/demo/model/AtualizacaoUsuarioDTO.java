package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtualizacaoUsuarioDTO {
	private String id;
    private PerfilUsuario perfil;
    private boolean ehAdmin;
    private String status;
}
