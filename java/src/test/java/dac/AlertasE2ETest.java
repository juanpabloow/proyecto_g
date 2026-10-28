package dac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dac.dominio.ConflictoDeDuplicado;
import dac.persistencia.AlertaRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de punta a punta de las historias de la segunda mitad del backlog
 * (HU-03, HU-05, HU-06 y HU-08) y de la interfaz web. Igual que
 * {@link ContratoE2ETest}: HTTP real contra la base de datos real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlertasE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AlertaRepo alertaRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiarBaseDeDatos() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("HU-03: el CSV de ejemplo trae un fraccionamiento en la Gobernación")
    void detecta_fraccionamiento_por_http() throws Exception {
        JsonNode cuerpo = cargarArchivo("contratos_ejemplo.csv");

        JsonNode fraccionamientos = cuerpo.get("alertasFraccionamiento");
        assertEquals(1, fraccionamientos.size(), "003 (45 M) y 005 (60 M): 27 días, suman 105 M");
        assertEquals("Gobernación Ejemplo", fraccionamientos.get(0).get("entidad").asText());
        assertEquals(2, fraccionamientos.get(0).get("evidencia").size());
    }

    @Test
    @DisplayName("HU-03: el CSV de fraccionamiento solo alerta lo que cabe en la ventana y la entidad")
    void fraccionamiento_respeta_ventana_y_entidad() throws Exception {
        JsonNode cuerpo = cargarArchivo("contratos_fraccionamiento_ejemplo.csv");

        JsonNode fraccionamientos = cuerpo.get("alertasFraccionamiento");
        assertEquals(1, fraccionamientos.size());
        assertEquals(3, fraccionamientos.get(0).get("evidencia").size(),
                "401, 402 y 403; el 404 es de otra entidad y el 405 está fuera de la ventana");
    }

    @Test
    @DisplayName("HU-05: nombres escritos distinto forman una sola alerta, persistida una vez")
    void nombres_variantes_por_http() throws Exception {
        JsonNode cuerpo = cargarArchivo("contratos_nombres_variantes_ejemplo.csv");

        assertEquals(1, cuerpo.get("alertas").size(), "301, 302 y 303 son el mismo par; 304 es LTDA");
        JsonNode alerta = cuerpo.get("alertas").get(0);
        assertEquals("Constructora Andina S.A.S.", alerta.get("contratista").asText(),
                "se muestra el nombre original del primer contrato");
        assertEquals(3, alerta.get("evidencia").size());

        cargarArchivo("contratos_nombres_variantes_ejemplo.csv");
        assertEquals(1, alertaRepo.count(), "recargar no duplica la alerta");
    }

    @Test
    @DisplayName("HU-06: un contrato ya guardado que llega con otro monto se reporta como conflicto")
    void conflicto_contra_la_base() throws Exception {
        cargarArchivo("contratos_ejemplo.csv");

        String corregido = "numero_contrato,entidad,contratista,funcionario,monto,fecha\n"
                + "001,Alcaldía de Ejemplo,ACME SAS,Juan Pérez,99000000,2026-01-15\n";
        ResponseEntity<String> respuesta = enviarCsv(texto(corregido));
        JsonNode cuerpo = JSON.readTree(respuesta.getBody());

        assertEquals(0, cuerpo.get("contratosNuevos").asInt(), "el contrato no se duplica (HU-07)");
        JsonNode conflictos = cuerpo.get("conflictos");
        assertEquals(1, conflictos.size(), "la corrección no se pierde en silencio");
        assertEquals(2, conflictos.get(0).get("fila").asInt());
        assertEquals(ConflictoDeDuplicado.YA_EN_LA_BASE, conflictos.get(0).get("conflictoCon").asText());
        assertEquals("monto", conflictos.get(0).get("camposDistintos").get(0).asText());
    }

    @Test
    @DisplayName("GET /api/alertas devuelve el estado actual sin volver a cargar")
    void consulta_el_estado_actual() throws Exception {
        cargarArchivo("contratos_ejemplo.csv");

        ResponseEntity<String> respuesta = rest.getForEntity("/api/alertas", String.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        JsonNode cuerpo = JSON.readTree(respuesta.getBody());
        assertEquals(5, cuerpo.get("totalEnBd").asInt());
        assertEquals(1, cuerpo.get("alertas").size());
        assertEquals(1, cuerpo.get("alertasFraccionamiento").size());
    }

    @Test
    @DisplayName("HU-08: exporta un CSV descargable con una fila por contrato de evidencia")
    void exporta_el_reporte() throws Exception {
        cargarArchivo("contratos_ejemplo.csv");

        ResponseEntity<byte[]> respuesta = rest.getForEntity("/api/alertas/exportar", byte[].class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getHeaders().getContentType().isCompatibleWith(MediaType.parseMediaType("text/csv")));
        assertTrue(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"),
                "el navegador debe descargarlo como archivo");
        String csv = new String(respuesta.getBody(), StandardCharsets.UTF_8);
        assertEquals(6, csv.split("\r\n").length, "encabezado + 3 de reincidencia + 2 de fraccionamiento");
    }

    @Test
    @DisplayName("un CSV inválido responde 400 con un mensaje que la interfaz puede mostrar")
    void error_con_mensaje() throws Exception {
        ResponseEntity<String> respuesta = enviarCsv(texto("numero_contrato,entidad\n001,X\n"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(JSON.readTree(respuesta.getBody()).get("error").asText().contains("Faltan columnas"),
                respuesta.getBody());
    }

    @Test
    @DisplayName("la interfaz web se sirve en la raíz")
    void sirve_la_interfaz() {
        ResponseEntity<String> respuesta = rest.getForEntity("/index.html", String.class);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().contains("Detector de Alertas de Contratación"));
    }

    private JsonNode cargarArchivo(String nombre) throws Exception {
        Path ruta = Path.of(System.getProperty("user.dir"), "..", "data", nombre).normalize();
        assertTrue(Files.exists(ruta), "No se encontró " + ruta.toAbsolutePath());
        ResponseEntity<String> respuesta = enviarCsv(new FileSystemResource(ruta.toFile()));
        assertEquals(HttpStatus.OK, respuesta.getStatusCode(), respuesta.getBody());
        return JSON.readTree(respuesta.getBody());
    }

    private static ByteArrayResource texto(String csv) {
        return new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "carga.csv";
            }
        };
    }

    private ResponseEntity<String> enviarCsv(Object archivo) {
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("archivo", archivo);
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/api/contratos/cargar", new HttpEntity<>(cuerpo, cabeceras), String.class);
    }
}
