package dac;

import dac.persistencia.ContratistaJpa;
import dac.persistencia.ContratistaRepo;
import dac.persistencia.ContratoJpa;
import dac.persistencia.ContratoRepo;
import dac.persistencia.EntidadJpa;
import dac.persistencia.EntidadRepo;
import dac.persistencia.FuncionarioJpa;
import dac.persistencia.FuncionarioRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R-8: "solo se rechaza un valor negativo" para el monto de un contrato.
 * Esta regla vive como restricción real de PostgreSQL
 * ({@code ck_contrato_monto CHECK (monto >= 0)} en {@code db/schema.sql}),
 * pero antes de esta prueba nadie la verificaba: ni {@code CargadorDeContratosTest}
 * ni {@code ContratoE2ETest} cargan un CSV con un monto negativo.
 *
 * <p>Usa el contenedor real de {@code docker-compose.yml} ({@code dac_db}) —
 * no un H2 en memoria — porque la restricción vive en el esquema SQL, no en
 * las anotaciones JPA de {@link ContratoJpa}, así que una base en memoria
 * generada por Hibernate no la tendría.
 */
@SpringBootTest
class RestriccionesDeBaseDeDatosTest {

    @Autowired
    private EntidadRepo entidadRepo;

    @Autowired
    private ContratistaRepo contratistaRepo;

    @Autowired
    private FuncionarioRepo funcionarioRepo;

    @Autowired
    private ContratoRepo contratoRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiarBaseDeDatos() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("R-8: un monto negativo lo rechaza la base de datos (ck_contrato_monto)")
    void rechaza_monto_negativo() {
        EntidadJpa entidad = entidadRepo.save(new EntidadJpa("Alcaldía de Prueba"));
        ContratistaJpa contratista = contratistaRepo.save(new ContratistaJpa("Contratista de Prueba"));
        FuncionarioJpa funcionario = funcionarioRepo.save(new FuncionarioJpa("Funcionario de Prueba"));

        ContratoJpa contratoInvalido = new ContratoJpa(
                "999", entidad, contratista, funcionario,
                new BigDecimal("-1.00"), LocalDate.now());

        Exception error = assertThrows(DataIntegrityViolationException.class,
                () -> contratoRepo.saveAndFlush(contratoInvalido));

        assertTrue(error.getMessage().toLowerCase().contains("ck_contrato_monto")
                        || error.getMessage().toLowerCase().contains("monto"),
                "el error debería venir de la restricción ck_contrato_monto: " + error.getMessage());
    }

    @Test
    @DisplayName("R-8, control: un monto en cero o positivo sí se acepta")
    void acepta_monto_cero_o_positivo() {
        EntidadJpa entidad = entidadRepo.save(new EntidadJpa("Alcaldía de Prueba"));
        ContratistaJpa contratista = contratistaRepo.save(new ContratistaJpa("Contratista de Prueba"));
        FuncionarioJpa funcionario = funcionarioRepo.save(new FuncionarioJpa("Funcionario de Prueba"));

        ContratoJpa contratoValido = new ContratoJpa(
                "999", entidad, contratista, funcionario,
                BigDecimal.ZERO, LocalDate.now());

        contratoRepo.saveAndFlush(contratoValido);

        assertTrue(contratoRepo.findByNumeroContratoAndEntidad("999", entidad).isPresent(),
                "un monto en cero es válido: la regla solo rechaza negativos");
    }
}
