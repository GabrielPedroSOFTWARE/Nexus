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
        if (problemaDeclarado == null || problemaDeclarado.trim().isEmpty()) {
            return 0;
        }

        String texto = problemaDeclarado.toLowerCase();

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

        return 0;
    }
}