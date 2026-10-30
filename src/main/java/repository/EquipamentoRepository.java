package repository;
import org.springframework.data.jpa.repository.JpaRepository;

import model.Equipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {}