package com.example.demo.model;

import java.util.List;

/** Dados seguros necessários para a tela de detalhes da ordem de serviço. */
public record PedidoDetalhesDTO(Solicitante solicitante, Equipamento equipamento) {

    public record Solicitante(String nome, String setor, String telefone, String cpfExibicao) {}

    public record Equipamento(
            Long id,
            String numeroSerie,
            String fabricante,
            String modelo,
            String processador,
            Integer ram,
            List<String> placasDeVideo,
            List<String> discos,
            String sistemaOperacional,
            boolean ehNotebook) {}
}
