package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
            "Só clique no link abaixo se você estiver na tela do sistema aguardando agora (válido por 15 minutos):\n" +
            link + "\n\n" +
            "Se não foi você quem pediu, basta ignorar este e-mail."
        );

        carteiro.send(mensagem);
    }
}