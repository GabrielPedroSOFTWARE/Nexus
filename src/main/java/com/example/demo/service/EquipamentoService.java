package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Usuario;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

@Service
public class EquipamentoService {

	public Equipamento gerarRelatorioCompleto(Usuario usuarioLogado) {
        
	    Equipamento equipamento = new Equipamento();
	    equipamento.setIdUsuario(usuarioLogado.getId());
	    
	    SystemInfo informacoes = new SystemInfo();
	    HardwareAbstractionLayer hardware = informacoes.getHardware();

	    equipamento.setFabricante(hardware.getComputerSystem().getManufacturer());
	    equipamento.setModelo(hardware.getComputerSystem().getModel());
	    equipamento.setProcessador(hardware.getProcessor().getProcessorIdentifier().getName());

	    long memoriaEmBytes = hardware.getMemory().getTotal();
	    int ramEmGb = (int) (memoriaEmBytes / (1024 * 1024 * 1024));
	    equipamento.setRam(ramEmGb);
	    
	    List<String> listaDePlacas = lerPlacasDeVideo(hardware.getGraphicsCards());
	    List<String> listaDeDiscos = lerDiscos(hardware.getDiskStores());

	    equipamento.setPlacasDeVideo(listaDePlacas);
	    equipamento.setDiscos(listaDeDiscos);
	    return equipamento;
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