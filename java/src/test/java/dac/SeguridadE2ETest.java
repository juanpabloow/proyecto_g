package dac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dac.persistencia.ContratoRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Módulo 7 — Pruebas de seguridad básicas: ataques simulados contra el
 * producto real (HTTP real + PostgreSQL real). Cada prueba corresponde a un
 * punto del checklist de docs/seguridad-y-rnf.md (S-01 a S-08).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class SeguridadE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ENCABEZADO = "numero_contrato,entidad,contratista,funcionario,monto,fecha\n";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ContratoRepo contratoRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limpiarBaseDeDatos() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    @Test
    @DisplayName("S-03 inyección SQL: el ataque se guarda como texto y las tablas siguen intactas")
    void inyeccion_sql_se_guarda_como_texto() throws Exception {
        String ataque = "x'); DROP TABLE contrato; --";
        String clasico = "' OR '1'='1";
        ResponseEntity<String> respuesta = enviar("ataque.csv", ENCABEZADO
                + "001,Alcaldía,\"" + ataque + "\",\"" + clasico + "\",100,2026-01-01\n"
                + "002,Alcaldía,\"" + ataque + "\",\"" + clasico + "\",100,2026-01-02\n");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode(), respuesta.getBody());
        assertEquals(2, contratoRepo.count(), "la tabla contrato sigue existiendo, con exactamente 2 filas");
        JsonNode alerta = JSON.readTree(respuesta.getBody()).get("alertas").get(0);
        assertEquals(ataque, alerta.get("contratista").asText(), "el texto llega como dato, no como SQL");
        assertEquals(clasico, alerta.get("funcionario").asText());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM contratista", Integer.class),
                "' OR '1'='1 no 'encontró' a otros contratistas");
    }

    @Test
    @DisplayName("S-04 XSS: un <script> en un nombre viaja como dato JSON, nunca como HTML")
    void xss_viaja_como_dato() {
        String script = "<script>alert(1)</script>";
        ResponseEntity<String> respuesta = enviar("xss.csv", ENCABEZADO
                + "001,Alcaldía," + script + ",Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía," + script + ",Juan Pérez,100,2026-01-02\n");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON),
                "la API responde JSON, que el navegador no ejecuta");
        assertEquals("nosniff", respuesta.getHeaders().getFirst("X-Content-Type-Options"),
                "y no se le permite al navegador adivinar que es HTML");
    }

    @Test
    @DisplayName("S-04 XSS: la interfaz nunca inserta datos como HTML (sin innerHTML ni eval)")
    void la_interfaz_inserta_texto_no_html() throws Exception {
        String app = new ClassPathResource("static/js/app.js").getContentAsString(StandardCharsets.UTF_8);

        assertFalse(app.contains("innerHTML"), "innerHTML ejecutaría un <script> que venga en el CSV");
        assertFalse(app.contains("outerHTML") || app.contains("insertAdjacentHTML") || app.contains("eval("),
                "ninguna otra vía de insertar HTML o código");
        assertTrue(app.contains("textContent"), "los datos se insertan como texto");
    }

    @Test
    @DisplayName("S-01 cabeceras de seguridad en la página y en la API")
    void cabeceras_de_seguridad() {
        HttpHeaders pagina = rest.getForEntity("/index.html", String.class).getHeaders();
        assertTrue(pagina.getFirst("Content-Security-Policy").contains("script-src 'self'"));
        assertTrue(pagina.getFirst("Content-Security-Policy").contains("frame-ancestors 'none'"));
        assertEquals("DENY", pagina.getFirst("X-Frame-Options"));
        assertEquals("nosniff", pagina.getFirst("X-Content-Type-Options"));
        assertEquals("no-referrer", pagina.getFirst("Referrer-Policy"));

        HttpHeaders api = rest.getForEntity("/api/alertas", String.class).getHeaders();
        assertEquals("no-store", api.getFirst(HttpHeaders.CACHE_CONTROL),
                "las alertas no quedan guardadas en cachés");
    }

    @Test
    @DisplayName("S-05 divulgación: los errores no revelan trazas, clases ni SQL")
    void errores_sin_detalles_internos() {
        ResponseEntity<String> invalido = enviar("malo.csv", "numero_contrato\n001\n");
        ResponseEntity<String> inexistente = rest.getForEntity("/api/no-existe?debug=true", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, invalido.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, inexistente.getStatusCode());
        for (String cuerpo : new String[]{invalido.getBody(), inexistente.getBody()}) {
            String texto = cuerpo == null ? "" : cuerpo;
            assertFalse(texto.contains("Exception"), texto);
            assertFalse(texto.contains("at dac.") || texto.contains("org.springframework"), texto);
            assertFalse(texto.toLowerCase().contains("select ") || texto.contains("postgres"), texto);
        }
    }

    @Test
    @DisplayName("S-02 frontera: solo se aceptan archivos .csv")
    void solo_csv() throws Exception {
        ResponseEntity<String> respuesta = enviar("programa.exe", ENCABEZADO + "001,A,B,C,1,2026-01-01\n");

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(JSON.readTree(respuesta.getBody()).get("error").asText().contains(".csv"));
        assertEquals(0, contratoRepo.count());
    }

    @Test
    @DisplayName("S-02 frontera: un archivo de más de 10 MB se rechaza con 413")
    void archivo_demasiado_grande() throws Exception {
        String relleno = "x".repeat(11 * 1024 * 1024);
        ResponseEntity<String> respuesta = enviar("grande.csv", ENCABEZADO + relleno);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, respuesta.getStatusCode());
        assertTrue(JSON.readTree(respuesta.getBody()).get("error").asText().contains("10 MB"));
        assertEquals(0, contratoRepo.count());
    }

    @Test
    @DisplayName("S-02 frontera: un monto negativo rechaza el archivo con mensaje, no con un 500")
    void monto_negativo() throws Exception {
        ResponseEntity<String> respuesta = enviar("negativo.csv", ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía,ACME SAS,Juan Pérez,-5,2026-01-02\n");

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(JSON.readTree(respuesta.getBody()).get("error").asText().contains("Fila 3"),
                "el mensaje dice en qué fila está el problema");
        assertEquals(0, contratoRepo.count(), "una carga rechazada no deja nada a medias");
    }

    @Test
    @DisplayName("S-02 frontera: un nombre más largo que la columna se descarta y la carga sigue")
    void nombre_demasiado_largo() throws Exception {
        ResponseEntity<String> respuesta = enviar("largo.csv", ENCABEZADO
                + "001,Alcaldía," + "A".repeat(300) + ",Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-02\n");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode(), "antes de esta corrección era un 500");
        assertEquals(1, JSON.readTree(respuesta.getBody()).get("filasDescartadas").size());
        assertEquals(1, contratoRepo.count());
    }

    @Test
    @DisplayName("S-07 auditoría: cada carga queda registrada, con cantidades y sin nombres de personas")
    void auditoria_sin_datos_personales(CapturedOutput salida) {
        enviar("auditoria.csv", ENCABEZADO
                + "001,Alcaldía,ACME SAS,Persona Secreta,100,2026-01-01\n"
                + "002,Alcaldía,ACME SAS,Persona Secreta,100,2026-01-02\n");
        enviar("rechazo.txt", "nada");

        String log = salida.getAll();
        assertTrue(log.contains("AUDITORIA carga aceptada archivo=\"auditoria.csv\""), "se registra la carga");
        assertTrue(log.contains("nuevos=2"), "con sus cantidades");
        assertTrue(log.contains("AUDITORIA carga rechazada"), "también se registran los rechazos");
        assertFalse(log.contains("Persona Secreta"), "el log no es otra copia de los datos sensibles");
    }

    @Test
    @DisplayName("S-06 secretos: la clave de la base no está escrita en el código")
    void la_clave_no_esta_en_el_codigo() throws Exception {
        String propiedades = new ClassPathResource("application.properties").getContentAsString(StandardCharsets.UTF_8);

        assertTrue(propiedades.contains("spring.datasource.password=${DB_PASSWORD}"),
                "la clave se lee de .env o de una variable de entorno");
        assertFalse(propiedades.contains("dac_pass"), "la clave anterior ya no está en el repositorio");
    }

    private ResponseEntity<String> enviar(String nombreArchivo, String contenido) {
        ByteArrayResource archivo = new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return nombreArchivo;
            }
        };
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("archivo", archivo);
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/api/contratos/cargar", new HttpEntity<>(cuerpo, cabeceras), String.class);
    }
}
