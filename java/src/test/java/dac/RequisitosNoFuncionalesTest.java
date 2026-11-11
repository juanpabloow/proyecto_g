package dac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Módulo 7 — Medición de los dos requisitos no funcionales declarados en
 * docs/seguridad-y-rnf.md. Corre en cada push y Pull Request (GitHub
 * Actions), así que el método y las condiciones son siempre los mismos y
 * cualquiera puede repetir la medición.
 *
 * <p>El resultado se escribe en {@code target/rnf/resultados.md}; el CI lo
 * publica en el resumen de la ejecución y lo guarda como artefacto.
 *
 * <p>Los datos son ficticios y se generan con una semilla fija: la misma
 * semilla produce siempre el mismo archivo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequisitosNoFuncionalesTest {

    /** Protocolo del curso: tres corridas, se descarta la primera. */
    static final int CORRIDAS = 3;
    static final int RNF1_CONTRATOS = 5_000;
    static final long RNF1_LIMITE_MS = 5_000;

    static final int RNF2_CONTRATOS = 1_000;
    static final int RNF2_RECARGAS = 10;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> INFORME = new ArrayList<>();
    private static final String USUARIO = "analista";       // credencial DEMO del perfil dev (application-dev.properties)
    private static final String CLAVE = "dac-demo-2026";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void baseVacia() {
        rest = rest.withBasicAuth(USUARIO, CLAVE); // S-10: toda petición va autenticada
        vaciar();
    }

    @Test
    @DisplayName("RNF-1 rendimiento: 5.000 contratos se cargan en 5 s o menos (peor de las corridas 2 y 3)")
    void rnf1_rendimiento_de_la_carga() throws Exception {
        byte[] csv = generarCsv(RNF1_CONTRATOS, 2026);

        // Protocolo del curso: tres corridas con la base vacía; la 1.ª se descarta
        // porque incluye el arranque en frío de la JVM y de Hibernate.
        List<Long> tiempos = new ArrayList<>();
        int alertas = 0;
        for (int i = 0; i < CORRIDAS; i++) {
            vaciar();
            long inicio = System.nanoTime();
            JsonNode r = cargar(csv);
            tiempos.add((System.nanoTime() - inicio) / 1_000_000);
            assertEquals(RNF1_CONTRATOS, r.get("contratosNuevos").asInt(), "todas las filas se cargan");
            alertas = r.get("alertas").size() + r.get("alertasFraccionamiento").size();
        }
        List<Long> validas = tiempos.subList(1, CORRIDAS);
        long peor = validas.stream().mapToLong(Long::longValue).max().orElseThrow();

        INFORME.add("## RNF-1 — Rendimiento de la carga\n\n"
                + "| Medida | Valor |\n| --- | --- |\n"
                + "| Operación | `POST /api/contratos/cargar` por HTTP real, hasta recibir la respuesta con las dos señales |\n"
                + "| Carga | 1 archivo de " + RNF1_CONTRATOS + " contratos, 1 usuario, base vacía en cada corrida |\n"
                + "| Datos | " + RNF1_CONTRATOS + " contratos ficticios (" + csv.length / 1024 + " KB, semilla 2026); producen " + alertas + " alertas |\n"
                + "| Corrida 1 (descartada) | " + tiempos.get(0) + " ms |\n"
                + "| Corrida 2 | " + tiempos.get(1) + " ms |\n"
                + "| Corrida 3 | " + tiempos.get(2) + " ms |\n"
                + "| Métrica | peor de las corridas 2 y 3: **" + peor + " ms** |\n"
                + "| Objetivo | ≤ " + RNF1_LIMITE_MS + " ms |\n"
                + "| Resultado | " + (peor <= RNF1_LIMITE_MS ? "✅ Cumple" : "❌ No cumple") + " |\n");

        assertTrue(peor <= RNF1_LIMITE_MS, "RNF-1: la peor de las corridas 2 y 3 tardó " + peor + " ms");
    }

    @Test
    @DisplayName("RNF-2 integridad: 10 recargas del mismo archivo no duplican nada (3 corridas)")
    void rnf2_integridad_ante_recargas() throws Exception {
        byte[] csv = generarCsv(RNF2_CONTRATOS, 7);

        StringBuilder filas = new StringBuilder();
        for (int corrida = 1; corrida <= CORRIDAS; corrida++) {
            vaciar();
            cargar(csv);
            long contratos = contar("contrato");
            long alertas = contar("alerta");
            long evidencia = contar("alerta_contrato");
            assertEquals(RNF2_CONTRATOS, contratos);
            assertTrue(alertas > 0, "el archivo debe producir alertas para que la prueba diga algo");

            int recargasSinCambios = 0;
            int nuevosEnRecargas = 0;
            for (int i = 0; i < RNF2_RECARGAS; i++) {
                JsonNode r = cargar(csv);
                nuevosEnRecargas += r.get("contratosNuevos").asInt();
                if (contar("contrato") == contratos && contar("alerta") == alertas
                        && contar("alerta_contrato") == evidencia) {
                    recargasSinCambios++;
                }
            }
            filas.append("| Corrida ").append(corrida).append(corrida == 1 ? " (descartada)" : "")
                 .append(" | ").append(contratos).append(" contratos, ").append(alertas).append(" alertas, ")
                 .append(evidencia).append(" filas de evidencia | ").append(nuevosEnRecargas)
                 .append(" | **").append(recargasSinCambios).append(" de ").append(RNF2_RECARGAS).append("** |\n");

            // La integridad no admite excepciones: se exige en las tres corridas.
            assertEquals(0, nuevosEnRecargas, "RNF-2: una recarga creó contratos nuevos (corrida " + corrida + ")");
            assertEquals(RNF2_RECARGAS, recargasSinCambios, "RNF-2: alguna recarga duplicó datos (corrida " + corrida + ")");
        }

        INFORME.add("## RNF-2 — Integridad ante recargas (idempotencia)\n\n"
                + "Operación: cargar un archivo de " + RNF2_CONTRATOS + " contratos ficticios (semilla 7) y "
                + "recargarlo " + RNF2_RECARGAS + " veces seguidas por HTTP real, contando `contrato`, `alerta` y "
                + "`alerta_contrato` tras cada recarga. Objetivo: 0 contratos nuevos y " + RNF2_RECARGAS + " de "
                + RNF2_RECARGAS + " recargas sin cambios. ✅ Cumple en las corridas 2 y 3.\n\n"
                + "| Corrida | Tras la 1.ª carga | Contratos nuevos en las recargas | Recargas sin ningún duplicado |\n"
                + "| --- | --- | --- | --- |\n" + filas);
    }

    @AfterAll
    static void escribirInforme() throws Exception {
        Runtime rt = Runtime.getRuntime();
        String entorno = "- **Fecha:** " + OffsetDateTime.now() + "\n"
                + "- **Commit:** " + commit() + "\n"
                + "- **Equipo:** " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ", "
                + rt.availableProcessors() + " núcleos, " + rt.maxMemory() / (1024 * 1024)
                + " MB de memoria para la JVM, Java " + System.getProperty("java.version")
                + ", PostgreSQL 16 en contenedor\n"
                + "- **Protocolo:** tres corridas, se descarta la primera; se informan las corridas 2 y 3\n";
        Path salida = Path.of("target", "rnf", "resultados.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, "# Medición de requisitos no funcionales\n\n" + entorno + "\n"
                + String.join("\n", INFORME), StandardCharsets.UTF_8);
        System.out.println(Files.readString(salida));
    }

    /** Hash corto del commit medido (en el CI, GITHUB_SHA), y si había cambios sin commit. */
    private static String commit() {
        String ci = System.getenv("GITHUB_SHA");
        if (ci != null && ci.length() >= 7) return ci.substring(0, 7) + " (GitHub Actions)";
        try {
            String hash = ejecutar("git", "rev-parse", "--short", "HEAD");
            String cambios = ejecutar("git", "status", "--porcelain", "--untracked-files=no");
            return hash + (cambios.isBlank() ? "" : " (con cambios locales sin commit)");
        } catch (Exception e) {
            return "desconocido";
        }
    }

    private static String ejecutar(String... comando) throws Exception {
        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        p.waitFor();
        return salida;
    }

    /**
     * Contratos ficticios reproducibles: 20 entidades, 600 contratistas y 250
     * funcionarios, montos entre 1 y 150 millones y fechas en 2025–2026. Con
     * esa mezcla aparecen reincidencias y fraccionamientos, como en un archivo real.
     */
    static byte[] generarCsv(int cantidad, long semilla) {
        Random azar = new Random(semilla);
        StringBuilder csv = new StringBuilder("numero_contrato,entidad,contratista,funcionario,monto,fecha\n");
        LocalDate inicio = LocalDate.of(2025, 1, 1);
        for (int i = 1; i <= cantidad; i++) {
            csv.append("RNF-").append(i).append(',')
               .append("Entidad ").append(1 + azar.nextInt(20)).append(',')
               .append("Contratista ").append(1 + azar.nextInt(600)).append(',')
               .append("Funcionario ").append(1 + azar.nextInt(250)).append(',')
               .append((1 + azar.nextInt(150)) * 1_000_000L).append(',')
               .append(inicio.plusDays(azar.nextInt(730))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private JsonNode cargar(byte[] csv) throws Exception {
        ByteArrayResource archivo = new ByteArrayResource(csv) {
            @Override
            public String getFilename() {
                return "rnf.csv";
            }
        };
        MultiValueMap<String, Object> cuerpo = new LinkedMultiValueMap<>();
        cuerpo.add("archivo", archivo);
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> respuesta = rest.postForEntity("/api/contratos/cargar",
                new HttpEntity<>(cuerpo, cabeceras), String.class);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode(), respuesta.getBody());
        return JSON.readTree(respuesta.getBody());
    }

    private void vaciar() {
        jdbc.execute("TRUNCATE TABLE alerta_contrato, alerta, contrato, "
                + "funcionario, contratista, entidad RESTART IDENTITY CASCADE");
    }

    private long contar(String tabla) {
        // Solo nombres fijos: el SQL no se arma con texto que venga de afuera.
        String sql = switch (tabla) {
            case "contrato" -> "SELECT count(*) FROM contrato";
            case "alerta" -> "SELECT count(*) FROM alerta";
            case "alerta_contrato" -> "SELECT count(*) FROM alerta_contrato";
            default -> throw new IllegalArgumentException("tabla no prevista: " + tabla);
        };
        return jdbc.queryForObject(sql, Long.class);
    }
}
