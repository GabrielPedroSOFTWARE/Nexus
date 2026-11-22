package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CriptografiaCpfService {

    private final SecretKeySpec chaveAes;
    private final SecretKeySpec chaveHmac;

    public CriptografiaCpfService(@Value("${app.criptografia.chave}") String chaveTexto) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] chaveBytes = sha.digest(chaveTexto.getBytes(StandardCharsets.UTF_8));

            this.chaveAes = new SecretKeySpec(chaveBytes, "AES");
            this.chaveHmac = new SecretKeySpec(chaveBytes, "HmacSHA256");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar chave de criptografia", e);
        }
    }

    // Tranca o CPF no cofre (o mesmo CPF sempre gera o mesmo texto embaralhado)
    public String criptografar(String texto) {
        if (texto == null) return null;
        try {
            byte[] iv = gerarEtiquetaParaTexto(texto);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, chaveAes, new IvParameterSpec(iv));

            byte[] criptografado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));

            // Junta a etiqueta (16 bytes) + texto trancado para conseguir abrir depois
            byte[] resultado = new byte[iv.length + criptografado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(criptografado, 0, resultado, iv.length, criptografado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado", e);
        }
    }
    
    public String descriptografarEMascarar(String cpfCriptografado) {
        if (cpfCriptografado == null || cpfCriptografado.isBlank()) return null;

        String cpfOriginal = descriptografar(cpfCriptografado);
        String numeros = cpfOriginal.replaceAll("\\D", "");
        if (numeros.length() != 11) {
            return "***.***.***-**";
        }

        return "***." + numeros.substring(3, 6) + "." + numeros.substring(6, 9) + "-**";
    }

    // Destranca o cofre e devolve o CPF original
    public String descriptografar(String textoCriptografado) {
        if (textoCriptografado == null) return null;
        try {
            byte[] dados = Base64.getDecoder().decode(textoCriptografado);
            byte[] iv = Arrays.copyOfRange(dados, 0, 16);
            byte[] conteudo = Arrays.copyOfRange(dados, 16, dados.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, chaveAes, new IvParameterSpec(iv));

            byte[] original = cipher.doFinal(conteudo);
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dado", e);
        }
    }

    // Cria uma etiqueta única de 16 bytes para cada CPF usando a chave secreta
    private byte[] gerarEtiquetaParaTexto(String texto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(chaveHmac);
        byte[] hash = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, 16);
    }
}