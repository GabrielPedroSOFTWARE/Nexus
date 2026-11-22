package com.example.demo.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;

@Service
public class PrioridadeService {

	@Autowired
    private PedidoRepository pedidoRepository;

	
	private static final int STATUS_CANCELADA = 6;

	// Risco físico / segurança 
	private static final List<String> PALAVRAS_EMERGENCIA = List.of(
	        "fumaca", "cheiro de queimado", "faisca", "curto", "choque", "pegando fogo"
	);

	// Parada total
	private static final List<String> PALAVRAS_CRITICAS = List.of(
	        "nao liga", "n liga", "queimou", "nao da video", "sem video",
	        "tela preta", "nao funciona", "nao inicia", "nao entra no windows"
	);

	// Problemas de uso/desempenho 
	private static final List<String> PALAVRAS_MEDIAS = List.of(
	        "lento", "lentidao", "tela azul", "trava", "travando",
	        "reiniciando", "desligando", "esquentando", "superaquecendo", "virus"
	);

	
	// Define a prioridade da O.S
	public Integer definirPrioridade(Pedido novoPedido) {
	    int pontuacao = calcularPontosDoProblema(novoPedido.getProblema());

	    LocalDateTime trintaDiasAtras = LocalDateTime.now().minusDays(30);
        long pedidosRecentes = pedidoRepository
                .countByIdPCAndStatusNotAndDataHoraAberturaAfter(
                        novoPedido.getIdPC(), STATUS_CANCELADA, trintaDiasAtras);

        if (pedidosRecentes >= 2) {
            pontuacao += 50;
        } else if (pedidosRecentes == 1) {
            pontuacao += 30;
        }

	    if (pontuacao >= 50) return 2;      
	    else if (pontuacao >= 30) return 1; 
	    else return 0;
	}

	
	// Calcula os pontos de prioridade com base em palavras-chave
	private int calcularPontosDoProblema(String problema) {
	    String textoCompleto = problema;

	    if (textoCompleto.isBlank()) return 0;

	    String texto = Normalizer.normalize(textoCompleto, Normalizer.Form.NFD)
	            .replaceAll("\\p{M}", "")
	            .toLowerCase();

	    if (PALAVRAS_EMERGENCIA.stream().anyMatch(texto::contains)) return 50;
	    if (PALAVRAS_CRITICAS.stream().anyMatch(texto::contains)) return 30;
	    if (PALAVRAS_MEDIAS.stream().anyMatch(texto::contains)) return 15;

	    return 0;
	}
}