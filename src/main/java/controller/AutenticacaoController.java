package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import model.Usuario;
import repository.UsuarioRepository;
import service.TokenService;

@RestController
public class AutenticacaoController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encriptador;

    // Realiza o login
    @PostMapping("/login")
    public String efetuarLogin(@RequestBody DadosLogin dados) {
    	UsernamePasswordAuthenticationToken tokenTemporario = 
    			new UsernamePasswordAuthenticationToken(dados.login(), dados.senha());
        Authentication autenticacao = manager.authenticate(tokenTemporario);
        String tokenJWT = tokenService.gerarToken((Usuario) autenticacao.getPrincipal());
        
        return tokenJWT;
    }
    
    // Cadastro temporário
    @PostMapping("/cadastrar")
    public String cadastrarUsuario(@RequestBody Usuario novoUsuario) {
        String senhaSegura = encriptador.encode(novoUsuario.getSenha());
        novoUsuario.setSenha(senhaSegura);
        usuarioRepository.save(novoUsuario);
        
        return "Utilizador registado com sucesso!";
    }
}

// Criação de classe de dados
record DadosLogin(String login, String senha) {}