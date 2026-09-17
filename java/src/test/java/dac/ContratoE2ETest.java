package dac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dac.persistencia.AlertaRepo;
import dac.persistencia.ContratoRepo;
import dac.persistencia.EntidadRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba única del esqueleto andante — contrato descrito en
 * esqueleto/rebanada.md, sección 4: {@code e2e_carga_persiste_y_es_idempotente}.
 *
 * <p>Entra por donde entra el actor externo del diagrama de secuencia:
 * {@code POST /api/contratos/cargar}. Usa {@code WebEnvironment.RANDOM_PORT},
 * es decir Tomcat embebido escuchando en un socket real — no MockMvc y ningún
 * doble de prueba, tal como exige la característica "real y ejecutable" del
 * esqueleto andante. Con esto la prueba cruza las cinco fronteras del
 * diagrama, incluida la de entrada externa (HTTP), que la versión anterior
 * de esta prueba dejaba fuera al invocar {@code ContratoServicio}
 * directamente.
 *
 * <p>Requiere el contenedor {@code dac_db} de {@code docker-compose.yml}
 * corriendo ({@code docker compose up -d db}): usa la base de datos real,
 * no una en memoria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RUTA = "/api/contratos/cargar";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private EntidadRepo entidadRepo;

    @Autowired
    private ContratoRepo contratoRepo;

    @Autowired
    private AlertaRepo alertaRepo;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Precondición del contrato: tablas entidad, contratista, funcionario y
     * contrato vacías. El contenedor de docker-compose siembra datos de
     * ejemplo al arrancar (db/datos.sql), así que hay que limpiarlos antes de
     * cada corrida — esto no es lógica de negocio, es preparación de la prueba.
     */
    @BeforeEach
    void limpiarBaseDeDatos() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    @Test
    void e2e_carga_persiste_y_es_idempotente() throws Exception {
        Path rutaCsv = rutaContratosEjemplo();
        assertTrue(Files.exists(rutaCsv),
                "No se encontró el CSV de ejemplo en " + rutaCsv.toAbsolutePath());

        // --- Primera ejecución --------------------------------------------------
        ResponseEntity<String> primera = enviarCsv(new FileSystemResource(rutaCsv.toFile()));

        assertEquals(HttpStatus.OK, primera.getStatusCode(), "Código HTTP de la primera carga");
        JsonNode cuerpo = JSON.readTree(primera.getBody());

        // Frontera de respuesta: el JSON que sale por HTTP
        assertEquals(5, cuerpo.get("contratosNuevos").asInt(), "Contratos nuevos en la primera carga");
        assertEquals(5, cuerpo.get("totalEnBd").asInt(), "Total en BD tras la primera carga");
        assertEquals(0, cuerpo.get("filasDescartadas").size(), "El CSV de ejemplo no tiene filas incompletas");
        assertEquals(0, cuerpo.get("duplicadosIgnorados").asInt(), "El CSV de ejemplo no trae duplicados");

        // Frontera de dominio: la señal del MVP viaja entera hasta el JSON (R-3, HU-02)
        assertEquals(1, cuerpo.get("alertas").size(), "Alertas detectadas sobre el histórico");
        JsonNode alerta = cuerpo.get("alertas").get(0);
        assertEquals("ACME SAS", alerta.get("contratista").asText());
        assertEquals("Juan Pérez", alerta.get("funcionario").asText());
        assertEquals(3, alerta.get("evidencia").size(), "Toda alerta trae su evidencia (HU-02)");

        // Frontera de persistencia: lo que quedó escrito en PostgreSQL
        assertEquals(5, contratoRepo.count(), "Filas en contrato tras la primera carga");
        assertEquals(2, entidadRepo.count(), "Entidades distintas tras la primera carga");

        // La alerta y su evidencia quedan guardadas, no solo devueltas (HU-02)
        assertEquals(1, alertaRepo.count(), "Alertas persistidas tras la primera carga");
        assertEquals(3, filasDeEvidencia(), "Contratos de evidencia persistidos");

        // Riesgo residual del contrato: no basta con que el COUNT cuadre, hay que
        // verificar que una fila concreta viajó con sus valores y sus FK intactas.
        assertTrue(contratoRepo.findAllConFetch().stream().anyMatch(c ->
                        c.getNumeroContrato().equals("003")
                                && c.getEntidad().getNombre().equals("Gobernación Ejemplo")
                                && c.getContratista().getNombre().equals("ACME SAS")
                                && c.getFuncionario().getNombre().equals("Juan Pérez")
                                && c.getMonto().compareTo(new java.math.BigDecimal("45000000.00")) == 0
                                && c.getFecha().toString().equals("2026-03-05")),
                "El contrato 003 de la Gobernación debe estar en BD con todos sus campos y FK");

        // --- Segunda ejecución (idempotencia — HU-07), sin limpiar la BD --------
        ResponseEntity<String> segunda = enviarCsv(new FileSystemResource(rutaCsv.toFile()));

        assertEquals(HttpStatus.OK, segunda.getStatusCode(), "Código HTTP de la segunda carga");
        JsonNode cuerpoSegunda = JSON.readTree(segunda.getBody());

        assertEquals(0, cuerpoSegunda.get("contratosNuevos").asInt(), "Contratos nuevos en la segunda carga");
        assertEquals(5, cuerpoSegunda.get("totalEnBd").asInt(), "Total en BD tras la segunda carga");
        assertEquals(5, contratoRepo.count(), "Filas en contrato: no debe haber duplicados");
        assertEquals(2, entidadRepo.count(), "Tampoco deben duplicarse las entidades");
        assertEquals(1, alertaRepo.count(), "uq_alerta_par: la alerta no se duplica");
        assertEquals(3, filasDeEvidencia(), "La evidencia tampoco se duplica");
    }

    /**
     * Ruta de error mínima del diagrama de secuencia: falta una columna
     * requerida (R-1), el dominio lanza IllegalArgumentException y el
     * controlador la traduce a 400 sin escribir nada en la base de datos.
     */
    @Test
    void rechaza_csv_sin_columnas_requeridas() {
        String csvMalo = "numero_contrato,entidad,contratista\n001,Alcaldía de Ejemplo,ACME SAS\n";
        ByteArrayResource archivo = new ByteArrayResource(csvMalo.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "incompleto.csv";
            }
        };

        ResponseEntity<String> respuesta = enviarCsv(archivo);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode(),
                "Un CSV sin las 6 columnas requeridas debe responder 400 (R-1)");
        assertEquals(0, contratoRepo.count(), "Una carga rechazada no debe escribir nada");
    }

    /**
     * R-6 y HU-04: las filas sin contratista o sin funcionario se descartan y
     * se reportan, pero el archivo sigue siendo válido (Decisión 12). Una fila
     * con monto o fecha vacíos NO se descarta: la señal no los usa (R-3, R-8)
     * y descartarla perdería reincidencias reales.
     *
     * <p>Esta prueba existe porque la rebanada destapó el caso: con monto y
     * fecha obligatorios en el modelo físico, la fila 107 rompía la carga
     * completa con un 400 — exactamente lo que la Decisión 12 descarta.
     */
    @Test
    void carga_filas_incompletas_sin_invalidar_el_archivo() throws Exception {
        Path rutaCsv = rutaData("contratos_incompletos_ejemplo.csv");
        assertTrue(Files.exists(rutaCsv), "No se encontró " + rutaCsv.toAbsolutePath());

        ResponseEntity<String> respuesta = enviarCsv(new FileSystemResource(rutaCsv.toFile()));

        assertEquals(HttpStatus.OK, respuesta.getStatusCode(),
                "Una fila incompleta no debe invalidar el archivo (Decisión 12)");
        JsonNode cuerpo = JSON.readTree(respuesta.getBody());

        // 4 contratos cargados: 101, 102, 103 y 107 (el de monto y fecha vacíos)
        assertEquals(4, cuerpo.get("contratosNuevos").asInt(), "Contratos cargados");
        assertEquals(4, cuerpo.get("totalEnBd").asInt(), "Total en BD");
        assertEquals(1, cuerpo.get("duplicadosIgnorados").asInt(),
                "La fila 9 repite la clave natural de la 2 (R-2)");

        // 3 descartadas: las filas 104, 105 y 106, cada una con su motivo (HU-04, criterio 2)
        JsonNode descartadas = cuerpo.get("filasDescartadas");
        assertEquals(3, descartadas.size(), "Filas descartadas reportadas");
        for (JsonNode fila : descartadas) {
            assertTrue(fila.get("fila").asInt() > 0, "Cada descarte indica el número de fila");
            assertTrue(fila.get("motivo").asText().contains("falta"), "Cada descarte indica por qué");
        }

        // La reincidencia real sobrevive al descarte (HU-04, criterio 1)
        assertEquals(1, cuerpo.get("alertas").size(), "La reincidencia real sigue apareciendo");
        JsonNode alerta = cuerpo.get("alertas").get(0);
        assertEquals("ACME SAS", alerta.get("contratista").asText());
        assertEquals("Juan Pérez", alerta.get("funcionario").asText());
        assertEquals(2, alerta.get("evidencia").size());

        // El contrato sin monto ni fecha quedó persistido, con NULL en esas columnas (R-6)
        assertTrue(contratoRepo.findAllConFetch().stream().anyMatch(c ->
                        c.getNumeroContrato().equals("107")
                                && c.getMonto() == null
                                && c.getFecha() == null),
                "El contrato 107 debe estar en BD: monto y fecha vacíos no descartan la fila (R-6)");
    }

    private int filasDeEvidencia() {
        return jdbc.queryForObject("SELECT count(*) FROM alerta_contrato", Integer.class);
    }

    private ResponseEntity<String> enviarCsv(Object archivo) {
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("archivo", archivo);

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);

        return rest.postForEntity(RUTA, new HttpEntity<>(cuerpo, cabeceras), String.class);
    }

    /** {@code data/contratos_ejemplo.csv}, un nivel arriba del módulo java/. */
    private static Path rutaContratosEjemplo() {
        return rutaData("contratos_ejemplo.csv");
    }

    private static Path rutaData(String nombre) {
        return Path.of(System.getProperty("user.dir"), "..", "data", nombre).normalize();
    }
}
