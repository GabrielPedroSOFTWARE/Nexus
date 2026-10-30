package service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;

import model.Usuario;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String senhaSecreta;

    // Método que cria o Token
    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(senhaSecreta);
            return JWT.create()
                    .withIssuer("API Nexus") // O nome de quem emitiu o token
                    .withSubject(usuario.getLogin()) // Quem é o dono do token
                    .withExpiresAt(dataExpiracao()) // Quando o token perde a validade
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    // Método que verifica se o token é verdadeira e devolve o nome do usuário
    public String getSubject(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(senhaSecreta);
            return JWT.require(algoritmo)
                    .withIssuer("API Nexus")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    // Data de expiração do token: 2 horas depois da emissão
    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
