package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.HistoricoPedido;
import com.example.demo.model.Pedido;
import com.example.demo.repository.HistoricoPedidoRepository;

@Service
public class HistoricoPedidoService {

    @Autowired
    private HistoricoPedidoRepository historicoRepository;
    private DateTimeFormatter formatadorDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    @Autowired
    private CriptografiaCpfService criptografiaService;

    
    // Registra o histórico da alteração do pedido
    public void registrarAcao(Pedido pedido, String cpfUsuario, String alteracao) {
        HistoricoPedido historico = new HistoricoPedido();
        
        historico.setIdPedido(pedido.getId());
        historico.setCpfUsuarioAutor(cpfUsuario);
        historico.setDataHora(formatadorDataHora.format(LocalDateTime.now()));
        historico.setApelido(pedido.getApelido());
        historico.setStatus(pedido.getStatus());
        historico.setPrioridade(pedido.getPrioridade());
        historico.setFrequencia(pedido.getFrequencia());
        historico.setProblema(pedido.getProblema());
        historico.setDataInicioManutencao(pedido.getDataInicioManutencao());
        historico.setDataFinalManutencao(pedido.getDataFinalManutencao());
        historico.setIdTecnico(pedido.getCpfTecnico());
        historico.setDescricao(pedido.getDescricao());
        historico.setObservacoesTecnico(pedido.getObservacoesTecnico());
        historico.setJustificativaCancelamento(pedido.getJustificativaCancelamento());
        historico.setAlteracao(alteracao);
        
        historicoRepository.save(historico);
    }
    
    // Retorna todo o histórico de alteração de um pedido
    public List<HistoricoPedido> buscarHistoricoDoPedido(String idPedido) {
        List<HistoricoPedido> historicos = historicoRepository
                .findByIdPedidoOrderByDataHoraDesc(idPedido);

        for (HistoricoPedido historico : historicos) {
            // Troque "CpfUsuario" pelo nome exato do campo na sua classe HistoricoPedido
            // (ex: getCpfResponsavel / setCpfResponsavelDescriptografado)
            if (historico.getCpfUsuarioAutor() != null && !historico.getCpfUsuarioAutor().isBlank()) {
                String cpfOriginal = criptografiaService.descriptografar(historico.getCpfUsuarioAutor());
                historico.setCpfUsuarioAutorDescriptografado(mascararCpf(cpfOriginal));
            }
        }

        return historicos;
    }
    
 // Transforma "12345678900" em "***.456.789-**"
 	private String mascararCpf(String cpfOriginal) {
 	    if (cpfOriginal == null) return null;

 	    String numeros = cpfOriginal.replaceAll("\\D", "");
 	    if (numeros.length() != 11) {
 	        return "***.***.***-**";
 	    }

 	    return "***." + numeros.substring(3, 6) + "." + numeros.substring(6, 9) + "-**";
 	}
}