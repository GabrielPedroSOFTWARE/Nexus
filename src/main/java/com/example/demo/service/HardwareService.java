package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

@Service
public class HardwareService {

	public Map<String, String> gerarRelatorioCompleto() {
        
	    // Criamos o nosso "dicionário" que vai guardar pares de textos (chave e valor)
	    Map<String, String> relatorio = new HashMap<>(); 
	    
	    SystemInfo informacoes = new SystemInfo();
	    HardwareAbstractionLayer hardware = informacoes.getHardware();

	    // Em vez de usar .set(), usamos o .put("Nome da Etiqueta", "Informação")
	    relatorio.put("Fabricante", hardware.getComputerSystem().getManufacturer());
	    relatorio.put("Modelo", hardware.getComputerSystem().getModel());
	    relatorio.put("Processador", hardware.getProcessor().getProcessorIdentifier().getName()); 

	    // Calcula a RAM
	    long memoriaEmBytes = hardware.getMemory().getTotal();
	    int ramEmGb = (int) (memoriaEmBytes / (1024 * 1024 * 1024));
	    // Transformamos o número inteiro da RAM em texto (String) para caber no Map
	    relatorio.put("RAM", String.valueOf(ramEmGb)); 
	    
	    // Pegamos as listas
	    List<String> listaDePlacas = lerPlacasDeVideo(hardware.getGraphicsCards());
	    List<String> listaDeDiscos = lerDiscos(hardware.getDiskStores());

	    // Juntamos os itens das listas em um único texto, separados por vírgula e espaço
	    relatorio.put("PlacasDeVideo", String.join(", ", listaDePlacas));
	    relatorio.put("Discos", String.join(", ", listaDeDiscos));

	    return relatorio;
	}

	// Os métodos de baixo continuam iguais, apenas servem de apoio para o método principal
	private List<String> lerPlacasDeVideo(List<GraphicsCard> gpus) {
	    List<String> placas = new ArrayList<>();
	    for (GraphicsCard gpu : gpus) {
	        placas.add(gpu.getName());
	    }
	    return placas; 
	}

	private List<String> lerDiscos(List<HWDiskStore> discos) {
	    List<String> lista = new ArrayList<>();
	    for (HWDiskStore disco : discos) {
	        long tamanho = disco.getSize() / (1024 * 1024 * 1024);
	        if (tamanho > 0) {
	            lista.add(disco.getModel() + " (" + tamanho + " GB)");
	        }
	    }
	    return lista;
	}
}