package dac.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FuncionarioRepo extends JpaRepository<FuncionarioJpa, Long> {
    Optional<FuncionarioJpa> findByNombre(String nombre);
}
