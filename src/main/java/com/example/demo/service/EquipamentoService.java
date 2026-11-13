package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

	
	// Gera o relatório do pc escaneado
	public Equipamento gerarRelatorioCompleto(Usuario usuarioLogado) {
        
		SystemInfo info = new SystemInfo();
	    HardwareAbstractionLayer hardware = info.getHardware();

	    Equipamento equipamento = new Equipamento();
	    
	    equipamento.setIdUsuario(usuarioLogado.getId());
	    equipamento.setSistemaOperacional(info.getOperatingSystem().toString());
	    equipamento.setFabricante(hardware.getComputerSystem().getManufacturer());
	    equipamento.setModelo(hardware.getComputerSystem().getModel());
	    equipamento.setProcessador(hardware.getProcessor().getProcessorIdentifier().getName());
	    equipamento.setRam((int) (hardware.getMemory().getTotal() / 1073741824L)); // 1024^3
	    equipamento.setPlacasDeVideo(lerPlacasDeVideo(hardware.getGraphicsCards()));
	    equipamento.setDiscos(lerDiscos(hardware.getDiskStores()));
	    
	    return equipamento;
	}

	
	// Retorna uma lista com as placas de vídeo lidas
	private List<String> lerPlacasDeVideo(List<GraphicsCard> gpus) {
	    List<String> placas = new ArrayList<>();
	    for (GraphicsCard gpu : gpus) {
	        placas.add(gpu.getName());
	    }
	    return placas; 
	}

	
	// Retorna uma lista com os discos lidos
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
	
	
	// Adiciona um novo equipamento ao banco de dados
	public void adicionarNovoEquipamento(Equipamento equipamentoEnviado, Usuario usuarioLogado) {
	    
	    String idUsuario = usuarioLogado.getId();
	    
	    Optional<Equipamento> pcExistente = equipamentoRepository
	            .findByIdUsuarioAndFabricanteAndModeloAndProcessador(
	                    idUsuario, 
	                    equipamentoEnviado.getFabricante(),
	                    equipamentoEnviado.getModelo(), 
	                    equipamentoEnviado.getProcessador()
	            );
	            
	    if (pcExistente.isPresent()) {
	        
	        Equipamento pcParaAtualizar = pcExistente.get();
	        
	        pcParaAtualizar.setRam(equipamentoEnviado.getRam());
	        pcParaAtualizar.setDiscos(equipamentoEnviado.getDiscos());
	        pcParaAtualizar.setPlacasDeVideo(equipamentoEnviado.getPlacasDeVideo());
	        
	        equipamentoRepository.save(pcParaAtualizar);
	        
	    } else {
	        equipamentoEnviado.setIdUsuario(idUsuario);
	        
	        equipamentoRepository.save(equipamentoEnviado);
	    }
	}
	
	
	// Altera um equipamento do banco de dados
	public void alterarEquipamento(Long id, Equipamento novoEquipamento) {
    	novoEquipamento.setId(id);
    	equipamentoRepository.save(novoEquipamento);
    }
	
	
	// Remove um equipamento do banco de dados
	public void removerEquipamento(Long idEquipamento, Usuario usuarioLogado) {
		
    	String idUsuario = usuarioLogado.getId();
    	boolean fazParte = equipamentoRepository
    			.verificarPossivelExclusaoEquipamento(idEquipamento, idUsuario);
    	
    	if(!fazParte) {
    		throw new RuntimeException("Há uma O.S em aberto para esse equipamento"); 
    	}
    	
    	equipamentoRepository.deleteById(idEquipamento);
    }
	
	
	// Retorna os equipamentos que podem receber uma O.S
	public List<Equipamento> retornarEquipamentosParaPedidos(String idUsuario) {
        return equipamentoRepository.buscarEquipamentosComStatusFinalizadoPorUsuario(idUsuario);
    }
	
	
	// Retorna os equipamentos por usuário
	public List<Equipamento> listarEquipamentosUsuario(Usuario usuarioLogado) {
	    return equipamentoRepository.findByIdUsuario(usuarioLogado.getId());
	}
	
	
	// Retorna os dados das estatísticas
	public Map<String, Long> retornarDadosEstatisticas() {
    	Map<String, Long> dados = new HashMap<String, Long>();
    	dados.put("totalUsuarios", usuarioRepository.count());
    	dados.put("totalMaquinas", equipamentoRepository.count());
    	return dados;
    }
}