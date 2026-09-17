package dac.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertaRepo extends JpaRepository<AlertaJpa, Long> {

    /** uq_alerta_par: una sola alerta por par y tipo. */
    Optional<AlertaJpa> findByContratistaAndFuncionarioAndTipo(
            ContratistaJpa contratista, FuncionarioJpa funcionario, String tipo);
}
