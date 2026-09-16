package dac;

import dac.aplicacion.ContratoServicio;
import dac.aplicacion.ResultadoServicio;
import dac.persistencia.ContratoRepo;
import dac.persistencia.EntidadRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba única del esqueleto andante — contrato descrito en
 * esqueleto/rebanada.md, sección 4: {@code e2e_carga_persiste_y_es_idempotente}.
 *
 * <p>El contrato original asume un punto de entrada CLI ({@code dac.Main})
 * que hoy no existe en el módulo Spring Boot; el punto de entrada real de
 * este módulo es {@link ContratoServicio#cargarYDetectar(InputStream)}
 * (el mismo método que expone {@code POST /api/contratos/cargar}), así que
 * la prueba lo invoca directamente en vez de lanzar un proceso `java -cp`.
 * Las aserciones sobre stdout ("Contratos nuevos: 5", "Total en BD: 5") se
 * traducen a sus equivalentes reales: los campos de {@link ResultadoServicio}
 * y los conteos de los repositorios JPA.
 *
 * <p>Requiere el contenedor {@code dac_db} de {@code docker-compose.yml}
 * corriendo ({@code docker compose up -d db}) — usa la base de datos real,
 * no un doble ni una base en memoria, tal como exige el contrato.
 */
@SpringBootTest
class ContratoServicioE2ETest {

    @Autowired
    private ContratoServicio contratoServicio;

    @Autowired
    private EntidadRepo entidadRepo;

    @Autowired
    private ContratoRepo contratoRepo;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Precondición del contrato: tablas entidad, contratista, funcionario y
     * contrato vacías. El contenedor de docker-compose siembra datos de
     * ejemplo en el arranque (db/datos.sql), así que hay que limpiarlos
     * antes de cada corrida — esto no es lógica de negocio, es preparación
     * de la prueba.
     */
    @BeforeEach
    void limpiarBaseDeDatos() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    @Test
    void e2e_carga_persiste_y_es_idempotente() throws IOException {
        Path rutaCsv = rutaContratosEjemplo();
        assertTrue(Files.exists(rutaCsv),
                "No se encontró el CSV de ejemplo en " + rutaCsv.toAbsolutePath());

        // --- Primera ejecución --------------------------------------------------
        ResultadoServicio primera;
        try (InputStream csv = new FileInputStream(rutaCsv.toFile())) {
            primera = contratoServicio.cargarYDetectar(csv);
        }

        assertEquals(5, primera.contratosNuevos(), "Contratos nuevos en la primera ejecución");
        assertEquals(5, primera.totalEnBd(), "Total en BD tras la primera ejecución");
        assertEquals(2, entidadRepo.count(), "Entidades distintas tras la primera ejecución");
        assertEquals(5, contratoRepo.count(), "Filas en contrato tras la primera ejecución");

        // --- Segunda ejecución (idempotencia — HU-07), sin limpiar la BD --------
        ResultadoServicio segunda;
        try (InputStream csv = new FileInputStream(rutaCsv.toFile())) {
            segunda = contratoServicio.cargarYDetectar(csv);
        }

        assertEquals(0, segunda.contratosNuevos(), "Contratos nuevos en la segunda ejecución");
        assertEquals(5, segunda.totalEnBd(), "Total en BD tras la segunda ejecución");
        assertEquals(5, contratoRepo.count(), "Filas en contrato: no debe haber duplicados");
    }

    /** {@code data/contratos_ejemplo.csv}, un nivel arriba del módulo java/. */
    private static Path rutaContratosEjemplo() {
        return Path.of(System.getProperty("user.dir"), "..", "data", "contratos_ejemplo.csv")
                .normalize();
    }
}
