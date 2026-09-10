package dac.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ContratoRepo extends JpaRepository<ContratoJpa, Long> {

    Optional<ContratoJpa> findByNumeroContratoAndEntidad(String numeroContrato, EntidadJpa entidad);

    @Query("SELECT c FROM ContratoJpa c JOIN FETCH c.entidad JOIN FETCH c.contratista JOIN FETCH c.funcionario")
    List<ContratoJpa> findAllConFetch();
}
