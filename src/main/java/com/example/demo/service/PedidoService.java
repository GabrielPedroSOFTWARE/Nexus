package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.Equipamento;
import com.example.demo.model.Pedido;
import com.example.demo.model.PedidoDetalhesDTO;
import com.example.demo.model.PerfilUsuario;
import com.example.demo.model.TecnicosAtivosDTO;
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
	
	private static final Sort ORDEM_PRIORIDADE = Sort.by(
	        Sort.Order.desc("prioridade"),
	        Sort.Order.asc("dataHoraAbertura")
	);
	
	
	
	// Cria uma nova O.S
	public void criarNovoPedido(Pedido novoPedido, MultipartFile arquivoFoto, Usuario usuario)
	        throws IOException {
	    String cpf = usuario.getCpf();
	    if (!equipamentoRepository.verificarManutencaoEmAberto(novoPedido.getIdPC(), cpf)) {
	        throw new RuntimeException("Há uma O.S em aberto para esse equipamento");
	    }

	    novoPedido.setId(gerarIdUnico());
	    novoPedido.definirDadosIniciais(cpf, arquivoFoto);
	    novoPedido.setPrioridade(prioridadeService.definirPrioridade(novoPedido));
	    novoPedido.setDataHoraAbertura(LocalDateTime.now());

	    pedidoRepository.save(novoPedido);
	    notificacaoBackOfficeService.enviarNotificacaoNovoPedido(novoPedido, "Nova O.S. aberta!");
	    historicoPedidoService.registrarAcao(novoPedido, cpf, "O.S criada");
	}

	
	// Gera um I.D Único para a O.S
	private String gerarIdUnico() {
	    String novoId;
	    do {
	        int numero = ThreadLocalRandom.current().nextInt(100_000_000);
	        novoId = String.format("OS-%d-%08d", Year.now().getValue(), numero);
	    } while (pedidoRepository.existsById(novoId));
	    return novoId;
	}

	
    // Altera o status da O.S
    @Transactional
    public void alterarStatusPedido(String id, Map<String, String> dados, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        int statusAntigo = pedido.getStatus();
        Map<String, String> valores = dados == null ? Map.of() : dados;
        String statusInformado = valores.get("novoStatusId");
        if (statusInformado == null || statusInformado.isBlank()) {
            throw new RuntimeException("Informe o novo status da O.S.");
        }
        int statusNovo = Integer.parseInt(statusInformado);
        validarAlteracaoStatus(pedido, statusNovo, usuario);

        LocalDate prazoAnterior = pedido.getDataFinalManutencao();
        LocalDate hoje = LocalDate.now();
        boolean estavaAtrasada = statusEmAberto(statusAntigo)
                && prazoAnterior != null && prazoAnterior.isBefore(hoje);
        boolean mudouStatus = statusAntigo != statusNovo;
        LocalDate prazoNovo = parsePrazo(valores.get("prazo"));
        String justificativaInformada = valores.get("justificativa");
        String justificativa = justificativaInformada == null ? "" : justificativaInformada.trim();
        if (justificativa.length() > 500) {
            throw new RuntimeException("A justificativa deve ter no máximo 500 caracteres.");
        }
        validarAlteracaoPrazo(statusAntigo, statusNovo, prazoAnterior,
                prazoNovo, estavaAtrasada, mudouStatus, justificativa, hoje);

        boolean prazoMudou = prazoNovo != null && !Objects.equals(prazoAnterior, prazoNovo);

        pedido.setStatus(statusNovo);
        pedido.registrarMovimentacao();
        atualizarDatasDoStatus(pedido, statusNovo);

        DescricaoPrazo detalhesNotificacao = aplicarDetalhesNotificacaoDoStatus(
                pedido, statusAntigo, statusNovo, valores);
        if (prazoNovo != null) {
            pedido.setDataFinalManutencao(prazoNovo);
            pedido.setAlertaPrazoEnviado(false);
            pedido.setEstahAtrasada(false);
            detalhesNotificacao = new DescricaoPrazo(
                    prazoMudou ? "Novo prazo definido. Justificativa: " + justificativa
                            : detalhesNotificacao.descricao(), prazoNovo);
        }

        notificacaoUsuarioService.enviarNotificacaoUsuario(
                pedido.getCpfUsuario(), statusNovo, detalhesNotificacao.prazo, detalhesNotificacao.descricao);
        String alteracaoHistorico = "Status: " + status.get(statusAntigo) + " -> " + status.get(statusNovo);
        if (prazoMudou) {
            String prazoAnteriorTexto = prazoAnterior == null ? "Não definido" : formatadorDataHora.format(prazoAnterior);
            alteracaoHistorico += " · Prazo: " + prazoAnteriorTexto + " -> " + formatadorDataHora.format(prazoNovo)
                    + " · Justificativa: " + justificativa;
        }
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracaoHistorico);
        pedidoRepository.save(pedido);
    }

    private void validarAlteracaoPrazo(int statusAntigo, int statusNovo,
            LocalDate prazoAnterior, LocalDate prazoNovo, boolean estavaAtrasada,
            boolean mudouStatus, String justificativa, LocalDate hoje) {
        boolean exigePrazoPelaTransicao = (statusAntigo < 2 && statusNovo == 2)
                || (statusAntigo > 1 && statusNovo == 1);
        if (exigePrazoPelaTransicao && prazoNovo == null) {
            throw new RuntimeException("Informe o prazo da manutenção.");
        }
        if (estavaAtrasada && mudouStatus) {
            if (prazoNovo == null || Objects.equals(prazoAnterior, prazoNovo) || prazoNovo.isBefore(hoje)) {
                throw new RuntimeException("Informe um novo prazo válido para a O.S. atrasada.");
            }
            if (justificativa.isBlank()) {
                throw new RuntimeException("Informe a justificativa para alterar o prazo.");
            }
        }
        if (prazoNovo != null && !Objects.equals(prazoAnterior, prazoNovo) && justificativa.isBlank()) {
            throw new RuntimeException("Informe a justificativa para alterar o prazo.");
        }
    }

    private boolean statusEmAberto(Integer valor) {
        return valor != null && valor >= 0 && valor <= 4;
    }

    private LocalDate parsePrazo(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return LocalDate.parse(valor.trim(), formatadorDataHora);
    }
    
    
    // Cancela a O.S
    public void cancelarPedido(String id, Usuario usuario, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        
        if(!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
        	throw new RuntimeException("Sem autorização para cancelar a O.S");
        }
        
        if(List.of(5, 6).contains(pedido.getStatus())) {
        	throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
        }
        
        pedido.setAlertaPrazoEnviado(true);
        pedido.setStatus(6);
        pedido.setJustificativaCancelamento(justificativa);
        
        pedidoRepository.save(pedido);
        String alteracao = "O.S cancelada";
        notificacaoUsuarioService
        	.enviarNotificacaoUsuario(pedido.getCpfUsuario(), 6, null, "Sua O.S foi cancelada");
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    }
    
    
    // Valida a alteração do status da O.S
    private void validarAlteracaoStatus(Pedido pedido, int statusNovo, Usuario usuario) {
        if (statusNovo < 0 || statusNovo >= status.size()) {
            throw new RuntimeException("O status informado é inválido.");
        }
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
        if(pedido.getStatus() < 2 && List.of(5, 6).contains(statusNovo )) {
        	throw new RuntimeException("Vá para a parte de manutenção primeiro");
        }
        if(pedido.getStatus() < 4 && statusNovo == 5) {
        	throw new RuntimeException("Deixe o ativo \"Pronto para entrega\" antes da etapa \"Concluída\"");
        }
    }

    
    // Atualiza os horários de controle conforme a nova etapa da O.S
    private void atualizarDatasDoStatus(Pedido pedido, int statusNovo) {
        if (statusNovo == 1) pedido.setDataHoraInicioDiagnostico(LocalDateTime.now());
        else if (statusNovo == 5) pedido.setDataHoraConclusao(LocalDateTime.now());
        if (statusNovo < 4) pedido.setDataHoraProntaRetirada(null);
    }
    
    
    // Aplica os detalhes para cada mudança de status
    private DescricaoPrazo aplicarDetalhesNotificacaoDoStatus(Pedido pedido, int antigo, int novo,
    		Map<String, String> dados) {
    	String descricao = "";
        if (antigo == 0 && novo == 1) {
            pedido.setObservacoesTecnico(dados.get("observacao"));
            descricao = dados.get("observacao");
        } else if ((antigo < 2 && novo == 2) || (antigo > 1 && novo == 1)) {
            LocalDate prazo = parsePrazo(dados.get("prazo"));
            descricao = (antigo > novo) ? "O.S voltou para fase de diagnóstico" : "Prazo definido";
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
    
    
    // Retorna os técnicos ativos
    public List<TecnicosAtivosDTO> retornarTecnicosAtivos() {
    	List<Usuario> tecnicos = 
        		usuarioRepository.findByPerfilNotAndStatus(PerfilUsuario.USER, "Ativo");
    	ArrayList<TecnicosAtivosDTO> listaTecnicos = new ArrayList<>();
    	for(Usuario tecnico : tecnicos) {
    		String cpfMascarado = criptografiaService.descriptografarEMascarar(tecnico.getCpf());
    		listaTecnicos.add(
    				new TecnicosAtivosDTO(tecnico.getCpf(),
    						cpfMascarado, tecnico.getNome())
    				);
    	}
    	return listaTecnicos;
    }
    
    
    // Atribui um técnico a uma O.S
    public void atribuirTecnico(String id, String cpfTecnico, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
        cpfTecnico = cpfTecnico.replace("\"", "").trim();
        validarAtribuicaoTecnico(pedido, cpfTecnico);

        Usuario novoTecnico = (Usuario) usuarioRepository.findByCpf(cpfTecnico);
        String descricao = montarDescricaoAtribuicao(pedido.getCpfTecnico(), novoTecnico);

        if (pedido.getDataHoraAtribuicaoTecnico() == null) {
            pedido.setDataHoraAtribuicaoTecnico(LocalDateTime.now());
        }
        pedido.setCpfTecnico(novoTecnico.getCpf());
        pedido.registrarMovimentacao();
        notificacaoBackOfficeService.enviarNotificacaoAtribuicao(
        		(Usuario) usuarioRepository.findByCpf(pedido.getCpfTecnico()),
        		novoTecnico,
        		pedido,
        		usuario);
        historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), descricao);
        pedidoRepository.save(pedido);
    }

    
    // Valida a atribuição do técnico
    private void validarAtribuicaoTecnico(Pedido pedido, String cpfNovoTecnico) {
        if (List.of(5, 6).contains(pedido.getStatus())) {
            throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
        }
        if (cpfNovoTecnico.equals(pedido.getCpfTecnico())) {
            throw new RuntimeException("Este técnico já está atribuído a essa O.S");
        }
    }

    
    // Monta a descrição da atribuição para notificação
    private String montarDescricaoAtribuicao(String cpfAntigo, Usuario novoTecnico) {
        if (cpfAntigo == null) {
            return "Técnico atribuído: " + novoTecnico.getNome();
        }
        Usuario antigoTecnico = (Usuario) usuarioRepository.findByCpf(cpfAntigo);
        return "Técnico: " + antigoTecnico.getNome() + " -> " + novoTecnico.getNome();
    }
    
    
    // Devolve a O.S para a fila
    @Transactional
    public void devolverParaAFila(String id, Usuario usuario, String justificativa) {
    	Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
    	
    	if(List.of(5, 6).contains(pedido.getStatus())) {
			throw new RuntimeException("A O.S já foi " + status.get(pedido.getStatus()));
		}
		if (pedido.getCpfTecnico() == null) {
			throw new RuntimeException("A O.S não possui técnico responsável.");
		}
		if (!usuario.isEhAdmin() && !usuario.getCpf().equals(pedido.getCpfTecnico())) {
			throw new RuntimeException("Sem autorização para devolver esta O.S à fila.");
		}
		String justificativaNormalizada = justificativa == null ? "" : justificativa.trim();
		if (justificativaNormalizada.isBlank()) {
			throw new RuntimeException("Informe a justificativa para devolver a O.S à fila.");
		}
		if (justificativaNormalizada.length() > 500) {
			throw new RuntimeException("A justificativa deve ter no máximo 500 caracteres.");
		}
    	
    	pedido.setCpfTecnico(null);
    	pedido.setStatus(0);
    	pedido.setDataFinalManutencao(null);
    	pedido.setAlertaPrazoEnviado(false);
    	pedido.registrarMovimentacao();
    	
    	String alteracao = "O.S de volta à fila\n"
				+ "Justificativa: " + justificativaNormalizada;
		pedidoRepository.save(pedido);
    	
    	notificacaoUsuarioService.enviarNotificacaoUsuario(pedido.getCpfUsuario(), 0, null, alteracao);
    	notificacaoBackOfficeService.enviarNotificacaoAtribuicao(null, null, pedido, usuario);
    	historicoPedidoService.registrarAcao(pedido, usuario.getCpf(), alteracao);
    	
    }

    @Transactional(readOnly = true)
    public PedidoDetalhesDTO obterDetalhesPedido(String id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "O.S. não encontrada."));

        Usuario solicitante = usuarioRepository.findByCpf(pedido.getCpfUsuario()) instanceof Usuario usuarioEncontrado
                ? usuarioEncontrado : null;
        Equipamento equipamento = pedido.getIdPC() == null ? null
                : equipamentoRepository.findById(pedido.getIdPC()).orElse(null);

        PedidoDetalhesDTO.Solicitante solicitanteDto = solicitante == null ? null
                : new PedidoDetalhesDTO.Solicitante(
                        solicitante.getNome(), solicitante.getSetor(), solicitante.getTelefone(),
                        criptografiaService.descriptografarEMascarar(solicitante.getCpf()));
        PedidoDetalhesDTO.Equipamento equipamentoDto = equipamento == null ? null
                : new PedidoDetalhesDTO.Equipamento(
                        equipamento.getId(), equipamento.getNumeroSerie(), equipamento.getFabricante(),
                        equipamento.getModelo(), equipamento.getProcessador(), equipamento.getRam(),
                        equipamento.getPlacasDeVideo(), equipamento.getDiscos(),
                        equipamento.getSistemaOperacional(), equipamento.isEhNotebook());
        return new PedidoDetalhesDTO(solicitanteDto, equipamentoDto);
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
        List<Pedido> pedidos = pedidoRepository.findByIdPC(id, ORDEM_PRIORIDADE);
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por usuário
    public List<Pedido> listarManutencoesUsuario(Usuario usuarioLogado) {
        List<Pedido> pedidos = pedidoRepository.findByCpfUsuario(usuarioLogado.getCpf(), ORDEM_PRIORIDADE);
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por técnico
    public List<Pedido> listarManutencoesAtribuidas(Usuario usuarioLogado) {
        List<Pedido> pedidos = pedidoRepository.findByCpfTecnicoAndStatusNotIn(
                usuarioLogado.getCpf(),
                List.of(5, 6),
                ORDEM_PRIORIDADE
        );
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista as O.Ss por frequência
    public List<Pedido> listarManutencoesPorFrequencia(int frequencia) {
        List<Pedido> pedidos = pedidoRepository.findByFrequencia(frequencia, ORDEM_PRIORIDADE);
        return prepararPedidosParaRetorno(pedidos);
    }

    // Lista todas as O.Ss
    public List<Pedido> listarTodasAsManutencoes() {
        List<Pedido> pedidos = pedidoRepository.findAll(ORDEM_PRIORIDADE);
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
            
            Usuario usuario = (Usuario) usuarioRepository.findByCpf(pedido.getCpfUsuario());
            pedido.setNomeUsuario(usuario == null ? "" : usuario.getNome());
            
            Usuario tecnico = (Usuario) usuarioRepository.findByCpf(pedido.getCpfTecnico());
            pedido.setNomeTecnico(tecnico == null ? "Atribuído" : tecnico.getNome());
            
            if(atrasada) {
            	notificacaoBackOfficeService.enviarNotificacaoPedidoAtrasado(pedido, tecnico);
            }
        }
        return pedidos;
    }
    public record DescricaoPrazo(String descricao, LocalDate prazo) {}
}
