package com.example.demo.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;

@Service
public class PrioridadeService {

	@Autowired
    private PedidoRepository pedidoRepository;

	
	// Sugestão de prioridade com base em reincidência
    public Integer sugerirPrioridadeInicial(Pedido novoPedido) {
        int pontuacao = calcularPontosDoProblema(novoPedido.getProblema());

        LocalDate trintaDiasAtras = LocalDate.now().minusDays(30);
        long pedidosRecentes = pedidoRepository.countByIdPCAndDataInicioManutencaoAfter(
                novoPedido.getIdPC(), trintaDiasAtras);
        
        if (pedidosRecentes > 0) pontuacao += 25;

        if (pontuacao >= 50) return 2;
        else if (pontuacao >= 30) return 1;
        else return 0;
        
    }
    
    
    // Calcula os pontos do problema
    private int calcularPontosDoProblema(String problemaDeclarado) {
    	if (problemaDeclarado == null || problemaDeclarado.isBlank()) return 0;

        String texto = Normalizer.normalize(problemaDeclarado, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        if (Stream.of("nao liga", "queimou", "fumaca",
        		"nao da video", "tela preta", "nao funciona")
                .anyMatch(texto::contains)) return 30;

        if (Stream.of("lento", "lentidao", "tela azul",
        		"trava", "reiniciando", "esquentando", "virus")
                .anyMatch(texto::contains)) return 15;

        return 0;
    }
}