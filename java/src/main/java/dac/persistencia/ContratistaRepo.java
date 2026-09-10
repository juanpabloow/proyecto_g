package dac.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContratistaRepo extends JpaRepository<ContratistaJpa, Long> {
    Optional<ContratistaJpa> findByNombre(String nombre);
}
