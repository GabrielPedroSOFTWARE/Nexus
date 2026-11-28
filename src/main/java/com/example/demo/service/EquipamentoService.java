package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Equipamento;
import com.example.demo.model.HistoricoUsuario;
import com.example.demo.model.Pedido;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.HistoricoUsuarioRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

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
	@Autowired
	private HistoricoUsuarioRepository historicoUsuarioRepository;
	@Autowired
	private NotificacaoUsuarioService notificacaoUsuarioService;

	
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
	
	
	private enum TipoSistemaPropriedade {
	    PCSYSTEMTYPE
	}
	
	
	// Verifica se é notebook ou não
	private boolean ehNotebook(HardwareAbstractionLayer hardware) {
	    try {
	        WmiQuery<TipoSistemaPropriedade> query =
	                new WmiQuery<>("Win32_ComputerSystem", TipoSistemaPropriedade.class);
	        WmiResult<TipoSistemaPropriedade> resultado =
	                WmiQueryHandler.createInstance().queryWMI(query);

	        if (resultado.getResultCount() > 0) {
	            int tipo = WmiUtil.getUint16(resultado, TipoSistemaPropriedade.PCSYSTEMTYPE, 0);
	            if (tipo == 2) {
	                return true;  // Notebook
	            }
	            if (tipo == 1 || tipo == 3) {
	                return false; // Desktop
	            }
	        }
	    } catch (Exception e) {}

	    String modelo = hardware.getComputerSystem().getModel();
	    if (modelo != null) {
	        modelo = modelo.toLowerCase();
	        if (modelo.contains("notebook") || modelo.contains("laptop") || modelo.contains("book")) {
	            return true;
	        }
	    }

	    return hardware.getPowerSources().stream().anyMatch(fonte -> {
	        String nome = (fonte.getName() + " " + fonte.getDeviceName()).toLowerCase();
	        boolean ehNobreak = nome.contains("ups") || nome.contains("nobreak") || nome.contains("hid");
	        boolean capacidadeDeNotebook = fonte.getDesignCapacity() > 100 || fonte.getMaxCapacity() > 100;
	        return !ehNobreak && capacidadeDeNotebook;
	    });
	}
	
	
	// Adiciona um novo equipamento ao banco de dados
	public void adicionarNovoEquipamento(Equipamento enviado, Usuario usuarioLogado) {
	    String cpfUsuario = usuarioLogado.getCpf();

	    Equipamento equipamentoExistente = equipamentoRepository
	            .findByNumeroSerie(enviado.getNumeroSerie())
	            .orElse(null);

	    if (equipamentoExistente != null) {
	        String cpfDonoAtual = equipamentoExistente.getCpfUsuario();

	        // Se já tem dono e NÃO é quem está tentando cadastrar, bloqueia
	        if (cpfDonoAtual != null && !cpfDonoAtual.equals(cpfUsuario)) {
	            throw new RuntimeException("Esse computador já está cadastrado no nome de outro usuário");
	        }

	        // Se estava sem dono (null) ou já era do próprio usuário, reaproveita a mesma ficha
	        equipamentoRepository.save(equipamentoExistente.atualizarHardware(enviado, cpfUsuario));
	        return;
	    }

	    // Se nunca existiu com esse número de série, mantém sua busca antiga ou cria um novo
	    Equipamento equipamento = equipamentoRepository
	            .findByCpfUsuarioAndFabricanteAndModeloAndProcessador(
	                    cpfUsuario, enviado.getFabricante(), enviado.getModelo(), enviado.getProcessador())
	            .orElse(enviado);

	    equipamentoRepository.save(equipamento.atualizarHardware(enviado, cpfUsuario));
	}
	
	
	// Altera um equipamento do banco de dados
	public void alterarEquipamento(Long id, Equipamento novoEquipamento) {
    	novoEquipamento.setId(id);
    	equipamentoRepository.save(novoEquipamento);
    }
	
	
	// Remove um equipamento do banco de dados
	public void removerEquipamento(Long idEquipamento, Usuario usuarioLogado, String motivo) {
		Equipamento equipamento = equipamentoRepository.findById(idEquipamento)
    			.orElseThrow(() -> new RuntimeException("Equipamento não encontrado"));
		
    	String cpfUsuario = usuarioLogado.getCpf();
    	
    	Usuario usuarioProprietario = 
				(Usuario) usuarioRepository.findByCpf(equipamento.getCpfUsuario());
    	
    	boolean naoTemPedidoAberto = equipamentoRepository
    			.verificarManutencaoEmAberto(idEquipamento, usuarioProprietario.getCpf());
    	
    	if(!naoTemPedidoAberto) {
    		throw new RuntimeException("Há uma O.S em aberto para esse equipamento"); 
    	}
    	
    	String alteracao = "O usuário " + usuarioLogado.getNome() 
    	+ " desvinculou o ativo PC-" + equipamento.getId() + " de sua conta";
    	if(usuarioLogado.isEhAdmin()) {
    		
    		alteracao = "O ADM " + usuarioLogado.getNome() 
    		+ " desvinculou o ativo PC-" + equipamento.getId() 
    		+ " da conta do usuário: " + usuarioProprietario.getNome() 
    		+ "\nMotivo: " + motivo;
    		
    		notificacaoUsuarioService
    		.enviarNotificacaoUsuario(usuarioProprietario.getCpf(), 99, null,
    				"O ADM desvinculou o " + "PC-" + equipamento.getId() + " da sua conta");
    	}
    	HistoricoUsuario historico = new HistoricoUsuario();
    	historico.setCpfUsuario(cpfUsuario);
    	historico.setDataHoraAlteracao(LocalDateTime.now());
    	historico.setDescricaoAlteracao(alteracao);
    	historicoUsuarioRepository.save(historico);
    	equipamento.setCpfUsuario(null);
    	equipamentoRepository.save(equipamento);
    }
	
	
	// Retorna os equipamentos por usuário
	public List<Equipamento> retornarEquipamentosPorUsuario(String cpfUsuario) {
		cpfUsuario = cpfUsuario.replace("\"", "");
		return equipamentoRepository.findByCpfUsuario(cpfUsuario);
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