package com.example.demo.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Pedido;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.PedidoRepository;
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
	@Autowired
	private CriptografiaCpfService criptografiaService;
	@Autowired
	private PedidoRepository pedidoRepository;

	
	// Gera o relatório do pc escaneado
	public Equipamento gerarRelatorioCompleto(Usuario usuarioLogado) {
        
		SystemInfo info = new SystemInfo();
	    HardwareAbstractionLayer hardware = info.getHardware();

	    Equipamento equipamento = new Equipamento();

	    equipamento.setNumeroSerie(lerNumeroSerie(hardware));
	    equipamento.setCpfUsuario(usuarioLogado.getCpf());
	    equipamento.setSistemaOperacional(info.getOperatingSystem().toString());
	    equipamento.setFabricante(hardware.getComputerSystem().getManufacturer());
	    equipamento.setModelo(hardware.getComputerSystem().getModel());
	    equipamento.setProcessador(hardware.getProcessor().getProcessorIdentifier().getName());
	    equipamento.setRam((int) (hardware.getMemory().getTotal() / 1073741824L));
	    equipamento.setPlacasDeVideo(lerPlacasDeVideo(hardware.getGraphicsCards()));
	    equipamento.setDiscos(lerDiscos(hardware.getDiskStores()));
	    equipamento.setEhNotebook(ehNotebook(hardware));
	    
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
	
	
	// Lê o número de série do equipamento
	private String lerNumeroSerie(HardwareAbstractionLayer hardware) {
	    String serial = hardware.getComputerSystem().getSerialNumber();

	    // Caso seja vago, procura o serial da placa mãe
	    if (ehSerialInvalido(serial)) {
	        serial = hardware.getComputerSystem().getBaseboard().getSerialNumber();
	    }

	    // Se mesmo assim for vago, procura o serial do hardware
	    if (ehSerialInvalido(serial)) {
	        serial = hardware.getComputerSystem().getHardwareUUID();
	    }

	    return serial;
	}

	
	// Verifica se o número de série é válido
	private boolean ehSerialInvalido(String texto) {
	    if (texto == null || texto.isBlank()) return true;
	    String minusculo = texto.toLowerCase();
	    return minusculo.equals("unknown")
	            || minusculo.equals("none")
	            || minusculo.contains("default")
	            || minusculo.contains("o.e.m.");
	}
	
	
	// Verifica se é notebook ou não
	private boolean ehNotebook(HardwareAbstractionLayer hardware) {
	    if (!hardware.getPowerSources().isEmpty()) {
	        return true;
	    }

	    String modelo = hardware.getComputerSystem().getModel();
	    if (modelo != null) {
	        modelo = modelo.toLowerCase();
	        if (modelo.contains("notebook") || modelo.contains("laptop") 
	        		|| modelo.contains("book")) {
	            return true;
	        }
	    }

	    return false;
	}
	
	
	// Adiciona um novo equipamento ao banco de dados
	public void adicionarNovoEquipamento(Equipamento enviado, Usuario usuarioLogado) {
	    String cpf = usuarioLogado.getCpf();

	    if (equipamentoRepository.existsByNumeroSerieAndCpfUsuarioNot(enviado.getNumeroSerie(), cpf)) {
	        throw new RuntimeException("Esse computador já está cadastrado no nome de outro usuário");
	    }

	    Equipamento equipamento = equipamentoRepository
	            .findByCpfUsuarioAndFabricanteAndModeloAndProcessador(
	                    cpf, enviado.getFabricante(), enviado.getModelo(), enviado.getProcessador())
	            .orElse(enviado);

	    equipamentoRepository.save(equipamento.atualizarHardware(enviado, cpf));
	}
	
	
	// Altera um equipamento do banco de dados
	public void alterarEquipamento(Long id, Equipamento novoEquipamento) {
    	novoEquipamento.setId(id);
    	equipamentoRepository.save(novoEquipamento);
    }
	
	
	// Remove um equipamento do banco de dados
	public void removerEquipamento(Long idEquipamento, Usuario usuarioLogado) {
    	String cpfUsuario = usuarioLogado.getCpf();
    	boolean naoTemPedidoAberto = equipamentoRepository
    			.verificarManutencaoEmAberto(idEquipamento, cpfUsuario);
    	
    	if(!naoTemPedidoAberto) {
    		throw new RuntimeException("Há uma O.S em aberto para esse equipamento"); 
    	}
    	
    	equipamentoRepository.deleteById(idEquipamento);
    }
	
	
	// Retorna os equipamentos que podem receber uma O.S
	public List<Equipamento> retornarEquipamentosParaPedidos(String cpfUsuario) {
	    List<Equipamento> equipamentos = equipamentoRepository
	            .buscarEquipamentosComStatusFinalizadoPorUsuario(cpfUsuario);
	    return preencherCpfMascarado(equipamentos);
	}

	// Retorna os equipamentos por usuário
	public List<Equipamento> listarEquipamentosUsuario(Usuario usuarioLogado) {
	    List<Equipamento> equipamentos = equipamentoRepository
	            .findByCpfUsuario(usuarioLogado.getCpf());
	    return preencherCpfMascarado(equipamentos);
	}

	// Percorre a lista, destranca o CPF e guarda a versão mascarada em cada equipamento
	private List<Equipamento> preencherCpfMascarado(List<Equipamento> equipamentos) {
	    for (Equipamento eq : equipamentos) {
	    	eq.setCpfUsuarioDescriptografado(
	    			criptografiaService.descriptografarEMascarar(eq.getCpfUsuario())
            		);
	    }
	    return equipamentos;
	}

	// Retorna os dados das estatísticas
	public Map<String, Long> retornarDadosEstatisticas() {
    	Map<String, Long> dados = new HashMap<String, Long>();
    	dados.put("totalUsuarios", usuarioRepository.countByPerfilNot(PerfilUsuario.PENDENT));
    	dados.put("totalMaquinas", equipamentoRepository.count());
    	return dados;
    }
	
	
	// Verifica se há alguma O.S aberta para o equipamento
	public boolean existePedidoEmAberto(Long idPC) {
		return pedidoRepository.existsByIdPCAndStatusNotIn(idPC, List.of(5, 6));
	}
	
	
	// Retorna a data da última manutenção concluída do PC
	public String retornarUltimaManutencao(Long idPC) {
	    boolean estaEmManutencao = pedidoRepository.existsByIdPCAndStatusNotIn(idPC, List.of(5, 6));
	    if (estaEmManutencao) {
	        return "Em manutenção";
	    }

	    Optional<Pedido> ultimoPedido = pedidoRepository
	            .findFirstByIdPCAndDataHoraConclusaoIsNotNullOrderByDataHoraConclusaoDesc(idPC);

	    if (ultimoPedido.isEmpty()) {
	        return "—";
	    }

	    DateTimeFormatter formatoBr = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	    return ultimoPedido.get().getDataHoraConclusao().format(formatoBr);
	}
}