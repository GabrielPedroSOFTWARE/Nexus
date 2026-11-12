package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.UsuarioRepository;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

@Service
public class EquipamentoService {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private EquipamentoRepository equipamentoRepository;

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
	
	public void adicionarNovoEquipamento(Equipamento equipamento, Usuario usuarioLogado) {
		equipamento.setIdUsuario(usuarioLogado.getId());
    	equipamentoRepository.save(equipamento);
	}
	
	public void alterarEquipamento(Long id, Equipamento novoEquipamento) {
    	novoEquipamento.setId(id);
    	equipamentoRepository.save(novoEquipamento);
    }
	
	public void removerEquipamento(Long idEquipamento, Usuario usuarioLogado) {
		
    	String idUsuario = usuarioLogado.getId();
    	boolean fazParte = equipamentoRepository
    			.verificarPossivelExclusaoEquipamento(idEquipamento, idUsuario);
    	
    	if(!fazParte) {
    		throw new RuntimeException("Há uma O.S em aberto para esse equipamento"); 
    	}
    	
    	equipamentoRepository.deleteById(idEquipamento);
    }
	
	public List<Equipamento> retornarEquipamentosParaPedidos(String idUsuario) {
        return equipamentoRepository.buscarEquipamentosComStatusFinalizadoPorUsuario(idUsuario);
    }
	
	public List<Equipamento> listarEquipamentosUsuario(Usuario usuarioLogado) {
	    return equipamentoRepository.findByIdUsuario(usuarioLogado.getId());
	}
	
	public Map<String, Long> retornarDadosEstatisticas(Usuario usuarioLogado) {
    	Map<String, Long> dados = new HashMap<String, Long>();
    	dados.put("totalUsuarios", usuarioRepository.count());
    	dados.put("totalMaquinas", equipamentoRepository.count());
    	return dados;
    }
}