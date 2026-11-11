package com.example.demo.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;

@Service
public class PrioridadeService {

	@Autowired
    private PedidoRepository pedidoRepository;

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
    
    private int calcularPontosDoProblema(String problemaDeclarado) {
        if (problemaDeclarado == null || problemaDeclarado.trim().isEmpty()) {
            return 0; // Sem descrição, ganha 0 pontos
        }

        String texto = problemaDeclarado.toLowerCase();

        // Nível Crítico = Ganha 30 pontos
        if (texto.contains("não liga") || 
            texto.contains("nao liga") || 
            texto.contains("queimou") || 
            texto.contains("fumaça") || 
            texto.contains("não dá vídeo") || 
            texto.contains("nao da video") || 
            texto.contains("tela preta") || 
            texto.contains("não funciona")) {
            
            return 30;
        }

        // Nível Médio = Ganha 15 pontos
        if (texto.contains("lento") || 
            texto.contains("lentidão") || 
            texto.contains("tela azul") || 
            texto.contains("travando") || 
            texto.contains("trava") || 
            texto.contains("reiniciando") || 
            texto.contains("esquentando") || 
            texto.contains("vírus")) {
            
            return 15;
        }

        // Se não achou nada crítico, é rotina = 0 pontos
        return 0;
    }
}