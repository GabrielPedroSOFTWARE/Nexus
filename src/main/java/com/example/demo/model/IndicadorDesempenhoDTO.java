package com.example.demo.model;

import java.util.List;

public record IndicadorDesempenhoDTO(
        String periodo,           
        Double mediaGeral, 
        List<PontoGraficoDTO> dados
) {}