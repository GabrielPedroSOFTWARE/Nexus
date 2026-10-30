package model;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Classe modelo do equipamento
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipamento {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String fabricante;
	private String modelo;
	private String processador;
	private int ram;
	@ElementCollection
	private List<String> placasDeVideo;
	@ElementCollection
	private List<String> discos;
	
}
