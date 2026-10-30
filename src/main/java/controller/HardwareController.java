package controller;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import model.Equipamento;
import model.Pedido;
import repository.EquipamentoRepository;
import repository.PedidoRepository;
import service.HardwareService;

@RestController
public class HardwareController {
	
	@Autowired
    private HardwareService hardwareService;
	@Autowired
    private EquipamentoRepository repositorioEquipamento;
	@Autowired
	private PedidoRepository repositorioPedido;

	// Salva o equipamento no banco de dados
    @PostMapping("/adicionar-equipamento")
    public String adicionarNovoEquipamento(@RequestBody Equipamento novoEquipamento) {
        repositorioEquipamento.save(novoEquipamento);
        return "Peça salva com sucesso no banco de dados!";
    }
    
    // Remove o equipamento pela id
    @DeleteMapping("/remover-equipamento/{id}")
    public String removerEquipamento(@PathVariable Long id) {
    	repositorioEquipamento.deleteById(id);
    	return "Peça deletada";
    }
    
    // Salva a solicitação no banco de dados
    @PostMapping("/adicionar-solicitacao")
    public String adicionarNovaSolicitacaoManutencao(@RequestBody Pedido pedido) {
    	repositorioPedido.save(pedido);
    	return "A solicitação foi adicionada";
    }

    // Realiza a verificação das peças do computador
    @GetMapping("/pecas")
    public Map<String, Object> verificarPecas() {
        return hardwareService.gerarRelatorioCompleto();
    }
    
    // Retorna os equipamentos cadastrados
	@GetMapping("/listar-equipamentos")
	public List<Equipamento> listarTodos() {
	    List<Equipamento> gavetaDeEquipamentos = repositorioEquipamento.findAll();
	    return gavetaDeEquipamentos;
	}
}
