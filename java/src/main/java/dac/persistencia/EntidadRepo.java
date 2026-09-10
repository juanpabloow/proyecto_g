package dac.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntidadRepo extends JpaRepository<EntidadJpa, Long> {
    Optional<EntidadJpa> findByNombre(String nombre);
}
