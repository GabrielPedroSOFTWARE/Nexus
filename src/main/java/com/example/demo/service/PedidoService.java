package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.EquipamentoRepository;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;

@Service
public class PedidoService {
	
	@Autowired
    private PedidoRepository pedidoRepository;
	@Autowired
	private NotificacaoUsuarioService notificacaoUsuarioService;
	@Autowired
	private NotificacaoBackOfficeService notificacaoBackOfficeService;
	@Autowired
	private PrioridadeService prioridadeService;
	@Autowired
	private HistoricoPedidoService historicoPedidoService;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private EquipamentoRepository equipamentoRepository;
	@Autowired
	private CriptografiaCpfService criptografiaService;
	
	private List<String> status = List.of("Recebida", "Em diagnóstico",
			"Em manutenção", "Aguardando peças", "Pronto para retirada",
			"Concluída", "Cancelada");
	
	private DateTimeFormatter formatadorDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	
	
	
	// Cria uma nova O.S
	public void criarNovoPedido(Pedido novoPedido, MultipartFile arquivoFoto, Usuario usuario) 
			throws IOException {
	    String cpf = usuario.getCpf();

	    if (!equipamentoRepository.verificarPossivelExclusaoEquipamento(novoPedido.getIdPC(), cpf)) {
	        throw new RuntimeException("Há uma O.S em aberto para esse equipamento");
	    }
	    String novoId;
	    do {
	        int numeroAleatorio = ThreadLocalRandom.current().nextInt(100_000_000);
	        novoId = "OS-" + Year.now().getValue() + "-" + String.format("%08d", numeroAleatorio);
	    } while (pedidoRepository.existsById(novoId));

	    novoPedido.setId(novoId);
	    
	    novoPedido.definirDadosIniciais(cpf, arquivoFoto);
	    novoPedido.setPrioridade(prioridadeService.definirPrioridade(novoPedido));
	    novoPedido.registrarMovimentacao();
	    novoPedido.setDataHoraAbertura(LocalDateTime.now());

	    pedidoRepository.save(novoPedido);
	    String mensagem = "Nova O.S. aberta!";
	    notificacaoBackOfficeService.enviarNotificacaoNovoPedido(novoPedido, mensagem);
	    historicoPedidoService.registrarAcao(novoPedido, cpf, "O.S criada");
	}

	
	// Altera uma O.S
	public void alterarPedido(Pedido novoPedido, MultipartFile arquivoFoto, String id, Usuario usuario) 
			throws IOException {
	    Pedido pedidoAtual = pedidoRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

	    validarAlteracao(pedidoAtual, usuario);

	    novoPedido.preservarDadosOriginais(pedidoAtual);
	    novoPedido.anexarFoto(arquivoFoto);
	    novoPedido.setPrioridade(prioridadeService.definirPrioridade(novoPedido));
	    novoPedido.registrarMovimentacao();

	    pedidoRepository.save(novoPedido);
	    historicoPedidoService.registrarAcao(pedidoAtual, usuario.getCpf(),
	    		"Mudança dos dados da O.S");
	}
	
	
	// Valida a alteração da O.S
	private void validarAlteracao(Pedido pedido, Usuario usuario) {
	    if (!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfUsuario())) {
	        throw new RuntimeException("Você não tem permissão para alterar essa O.S");
	    }
	    if (List.of(5, 6).contains(pedido.getStatus())) {
	        throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
	    }
	}
	
	
	// Cancela a O.S
    public void cancelarPedido(String id, Usuario usuario, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para cancelar a O.S");
        }
        
        if(pedido.getStatus() == 6) {
        	throw new RuntimeException("A O.S já foi cancelada");
        }
        
        pedido.setAlertaPrazoEnviado(true);
        pedido.setStatus(6);
        pedido.setJustificativaCancelamento(justificativa);
        
        pedidoRepository.save(pedido);
        String alteracao = "O.S cancelada";
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    }
    
    
    // Altera o status da O.S
    public void alterarStatusPedido(String id, Map<String, String> dados, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        int statusAntigo = pedido.getStatus();
        int statusNovo = Integer.parseInt(dados.get("novoStatusId"));

        validarAlteracaoStatus(pedido, statusNovo, usuario);

        pedido.setStatus(statusNovo);
        DescricaoPrazo dadosDetalhesStatus = 
        		aplicarDetalhesDoStatus(pedido, statusAntigo, statusNovo, dados); 
        LocalDate novoPrazo = dadosDetalhesStatus.prazo;
        if(novoPrazo != null) pedido.setAlertaPrazoEnviado(false);
        pedido.registrarMovimentacao();
        
        if(statusNovo == 1) pedido.setDataHoraInicioDiagnostico(LocalDateTime.now());
        else if(statusNovo == 5) pedido.setDataHoraConclusao(LocalDateTime.now());
        
        if (statusNovo < 4) {
            pedido.setDataHoraProntaRetirada(null);
        }
        
        String descricao = dadosDetalhesStatus.descricao;
        
        if(pedido.isEstahAtrasada()) {
        	pedido.setDataFinalManutencao(LocalDate.parse(dados.get("prazo"), formatadorDataHora));
        	descricao = dados.get("justificativa");
        	pedido.setEstahAtrasada(false);
        }
        
        notificacaoUsuarioService
        	.enviarNotificacaoUsuario(pedido.getCpfUsuario(), statusNovo, novoPrazo, descricao);
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(),
                "Status: " + status.get(statusAntigo) + " -> " + status.get(statusNovo));
        pedidoRepository.save(pedido);
    }
    
    
    // Valida a alteração do status da O.S
    private void validarAlteracaoStatus(Pedido pedido, int statusNovo, Usuario usuario) {
        if (List.of(5, 6).contains(pedido.getStatus())) {
            throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
        }
        if (pedido.getCpfTecnico() == null) {
            throw new RuntimeException("Defina primeiro um técnico antes de alterar o status da O.S");
        }
        if (!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
            throw new RuntimeException("Sem autorização para alterar o status da O.S");
        }
        if (statusNovo == 0) {
            throw new RuntimeException("Status só pode voltar para o início ao devolvê-lo à fila");
        }
    }

    
    // Aplica os detalhes para cada mudança de status
    private DescricaoPrazo aplicarDetalhesDoStatus(Pedido pedido, int antigo, int novo,
    		Map<String, String> dados) {
    	String descricao = "";
        if (antigo == 0 && novo == 1) {
            pedido.setObservacoesTecnico(dados.get("observacao"));
            descricao = dados.get("observacao");
        } else if ((antigo < 2 && novo == 2) || (antigo > 1 && novo == 1)) {
            LocalDate prazo = LocalDate.parse(dados.get("prazo"), formatadorDataHora);
            pedido.setDataFinalManutencao(prazo);
            pedido.setAlertaPrazoEnviado(false);
            descricao = pedido.getObservacoesTecnico();
            return new DescricaoPrazo(descricao, prazo);
        } else if (novo == 5) {
            pedido.setObservacoesTecnico(dados.get("observacao"));
            pedido.setDescricao(dados.get("descricao"));
            descricao = dados.get("descricao");
        } else if (antigo > novo) {
            pedido.setJustificativa(dados.get("justificativa"));
            descricao = "Justificativa para retornar operação: " + dados.get("justificativa");
        }
        return new DescricaoPrazo(descricao, null);
    }
    
    
    // Atribui um técnico a uma O.S
    public void atribuirTecnico(String id, String cpfTecnico, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (List.of(5, 6).contains(pedido.getStatus())) {
            throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
        }
        cpfTecnico = cpfTecnico.replace("\"", "").trim();
        Usuario novoTecnico = (Usuario) usuarioRepository.findByCpf(cpfTecnico);

        String alteracao = "Técnico atribuído: " + novoTecnico.getNome();
        
        if (pedido.getDataHoraAtribuicaoTecnico() == null) {
            pedido.setDataHoraAtribuicaoTecnico(LocalDateTime.now());
        }

        if (pedido.getCpfTecnico() != null) {
            Usuario antigoTecnico = (Usuario) usuarioRepository
            		.findByCpf(pedido.getCpfTecnico());
            if(antigoTecnico.getCpf().equals(cpfTecnico)) {
            	throw new RuntimeException("Este técnico já está atribuído a essa O.S");
            }
            alteracao = "Técnico: " + antigoTecnico.getNome() + " -> " + novoTecnico.getNome();
        }
        pedido.registrarMovimentacao();

        pedido.setCpfTecnico(novoTecnico.getCpf());
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
        pedidoRepository.save(pedido);
    }
    
    
    // Devolve a O.S para a fila
    public void devolverParaAFila(String id, Usuario usuario, String justificativa) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
    	
    	pedido.setCpfTecnico(null);
    	pedido.setStatus(0);
    	pedido.setDataFinalManutencao(null);
    	pedido.setAlertaPrazoEnviado(false);
    	pedido.registrarMovimentacao();
    	
    	String alteracao = "O.S de volta à fila\n"
    			+ "Justificativa: " + justificativa;
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    }
    
    
    // Definir/Alterar a justificativa de uma O.S
    public void definirJustificativa(String id, String justificativa, Usuario usuario) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para alterar o status da O.S");
        }
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
    	
    	String antigaJustificativa = pedido.getJustificativa();
    	
    	pedido.setJustificativaCancelamento(justificativa);
    	
    	String alteracao = (antigaJustificativa != null && !antigaJustificativa.isEmpty()) ?
    			"Alteração de justificativa" : "Definição de justificativa";
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    	pedidoRepository.save(pedido);
    }
    
    
    // Realiza as obervações do técnico
    public void realizarObservacoes(String id, Usuario usuario, String observacoes) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para alterar o status da O.S");
        }
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
    	
    	if(pedido.getStatus() != 2) {
    		throw new RuntimeException("Só é possível adicionar uma observação na fase de diagnóstico");
    	}
    	
    	pedido.setObservacoesTecnico(observacoes);
    	
    	String alteracao = "Observações do técnico adicionadas";
    	
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    	pedidoRepository.save(pedido);
    }
    
    
    // Lista as O.Ss por equipamento
    public List<Pedido> listarManutencoesEquipamento(Long id) {
        List<Pedido> pedidos = pedidoRepository.findByIdPC(id);
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por usuário
    public List<Pedido> listarManutencoesUsuario(Usuario usuarioLogado) {
        List<Pedido> pedidos = pedidoRepository.findByCpfUsuario(usuarioLogado.getCpf());
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por técnico
    public List<Pedido> listarManutencoesAtribuidas(Usuario usuarioLogado) {
        List<Pedido> pedidos = pedidoRepository.findByCpfTecnicoAndStatusNotIn(
                usuarioLogado.getCpf(),
                List.of(5, 6)
        );
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por frequência
    public List<Pedido> listarManutencoesPorFrequencia(int frequencia) {
        List<Pedido> pedidos = pedidoRepository.findByFrequencia(frequencia);
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista todas as O.Ss
    public List<Pedido> listarManutencoesAdmin() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        return prepararPedidosParaRetorno(pedidos);
    }

    // Método auxiliar que percorre qualquer lista de pedidos e mascara os dois CPFs
    private List<Pedido> prepararPedidosParaRetorno(List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            pedido.setCpfUsuarioDescriptografado(
                    criptografiaService.descriptografarEMascarar(pedido.getCpfUsuario())
            );
            pedido.setCpfTecnicoDescriptografado(
                    criptografiaService.descriptografarEMascarar(pedido.getCpfTecnico())
            );
            boolean emAberto = pedido.getStatus() == null
                    || (pedido.getStatus() != 5 && pedido.getStatus() != 6);

            boolean atrasada = emAberto
                    && pedido.getDataFinalManutencao() != null
                    && pedido.getDataFinalManutencao().isBefore(LocalDate.now());

            pedido.setEstahAtrasada(atrasada);
        }
        return pedidos;
    }
    public record DescricaoPrazo(String descricao, LocalDate prazo) {}
}
