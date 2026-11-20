package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.DadosLogin;
import com.example.demo.model.EsqueciSenhaDTO;
import com.example.demo.model.RedefinirSenhaDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.AutenticacaoService;
import com.example.demo.service.UsuarioService;

@RestController
public class AutenticacaoController {

	@Autowired
	private AutenticacaoService autenticacaoService;
	@Autowired
	private UsuarioService usuarioService;

	
    // Realiza o login
    @PostMapping("/login")
    public ResponseEntity<?> efetuarLogin(@RequestBody DadosLogin dados) {
        try {
        	return autenticacaoService.efetuarLogin(dados);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cpf ou senha errados");
        }
    }
    
    
    // Cadastro temporário
    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrarUsuario(@RequestBody Usuario novoUsuario) {
    	try {
    		usuarioService.cadastrarUsuario(novoUsuario);
            return ResponseEntity.ok("Solicitação enviada com sucesso!");
    	} catch(RuntimeException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
    	}
    	
    }
    
    
    // Envia o e-mail com o link de recuperação
    @PostMapping("/esqueci-senha")
    public ResponseEntity<?> esqueciSenha(@RequestBody EsqueciSenhaDTO dados) {
        try {
            String codigoEspera = usuarioService.solicitarRecuperacaoSenha(dados.cpf());
            return ResponseEntity.ok(Map.of("codigoEspera", codigoEspera));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Rotina que ocorre quando a pessoa clica no link
    @GetMapping(value = "/confirmar-recuperacao", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> confirmarLink(@RequestParam("token") String token) {
        try {
            usuarioService.confirmarCliqueNoLink(token);
            return ResponseEntity.ok(
                "<h2>E-mail confirmado!</h2>"
                + "<p>Você já pode voltar para a tela do sistema e criar sua nova senha.</p>"
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("<h2>Link inválido ou expirado.</h2>");
        }
    }

    // Verifica se o usuário clicou no link
    @GetMapping("/status-recuperacao")
    public ResponseEntity<?> statusRecuperacao(@RequestParam("codigo") String codigo) {
        boolean confirmado = usuarioService.verificarSeConfirmou(codigo);
        return ResponseEntity.ok(Map.of("confirmado", confirmado));
    }

    // Redefine a senha enviando o código de espera e a nova senha
    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody RedefinirSenhaDTO dados) {
        try {
            usuarioService.redefinirSenha(dados.codigoEspera(), dados.novaSenha());
            return ResponseEntity.ok("Senha alterada com sucesso!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}