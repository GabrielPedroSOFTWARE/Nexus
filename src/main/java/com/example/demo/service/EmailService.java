package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.model.PerfilUsuario;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender carteiro;

    public void enviarEmailRecuperacao(String emailDestino, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(emailDestino);
        mensagem.setSubject("Recuperação de Senha");
        mensagem.setText(
            "Olá! Recebemos um pedido para alterar a sua senha.\n\n" +
            "Só clique no link abaixo se você estiver na tela do sistema"
            + " aguardando agora (válido por 30 minutos):\n" +
            link + "\n\n" +
            "Se não foi você quem pediu, basta ignorar este e-mail."
        );

        carteiro.send(mensagem);
    }
    
    
    // Envia um e-mail ao usuário solicitante caso a solicitação seja aceita
    public void enviarEmailSolicitacaoAceita(String emailDestino, String nomeUsuario, PerfilUsuario perfil) {
        String nomePerfil = switch (perfil) {
            case USER -> "Usuário";
            case SUPPORT_MANAGER -> "Técnico / Gestor";
            default -> perfil.name();
        };

        String corpo = """
                Olá, %s!

                Sua solicitação de cadastro no sistema foi aceita.
                Perfil atribuído: %s

                Você já pode fazer login usando seu CPF e a senha cadastrada.
                """.formatted(nomeUsuario, nomePerfil);

        enviarEmail(emailDestino, "Sua solicitação de acesso foi aprovada!", corpo);
    }

    
    // Envia um e-mail ao usuário solicintante caso a solicitação seja negada
    public void enviarEmailSolicitacaoNegada(String emailDestino, String nomeUsuario) {
        String corpo = """
                Olá, %s.

                Informamos que sua solicitação de cadastro no sistema não foi aprovada pela administração.

                Caso acredite que houve algum engano ou precise de mais informações, entre em contato com o administrador do sistema.
                """.formatted(nomeUsuario);

        enviarEmail(emailDestino, "Sua solicitação de acesso não foi aprovada", corpo);
    }
    
    
    // E-mail que será enviado caso a solicitação seja negada ou não
    private void enviarEmail(String destino, String assunto, String corpo) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(destino);
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        carteiro.send(mensagem);
    }
}