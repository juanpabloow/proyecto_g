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

    static final int RNF1_CONTRATOS = 5_000;
    static final long RNF1_LIMITE_MS = 5_000;
    static final int RNF1_CORRIDAS = 3;

    static final int RNF2_CONTRATOS = 1_000;
    static final int RNF2_RECARGAS = 10;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> INFORME = new ArrayList<>();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void baseVacia() {
        vaciar();
    }

    @Test
    @DisplayName("RNF-1 rendimiento: 5.000 contratos se cargan en 5 s o menos (peor de 3 corridas)")
    void rnf1_rendimiento_de_la_carga() throws Exception {
        byte[] csv = generarCsv(RNF1_CONTRATOS, 2026);

        cargar(csv); // calentamiento: la primera carga incluye el arranque en frío de la JVM
        List<Long> tiempos = new ArrayList<>();
        int alertas = 0;
        for (int i = 0; i < RNF1_CORRIDAS; i++) {
            vaciar();
            long inicio = System.nanoTime();
            JsonNode r = cargar(csv);
            tiempos.add((System.nanoTime() - inicio) / 1_000_000);
            assertEquals(RNF1_CONTRATOS, r.get("contratosNuevos").asInt(), "todas las filas se cargan");
            alertas = r.get("alertas").size() + r.get("alertasFraccionamiento").size();
        }
        long peor = tiempos.stream().mapToLong(Long::longValue).max().orElseThrow();
        long mejor = tiempos.stream().mapToLong(Long::longValue).min().orElseThrow();

        INFORME.add("## RNF-1 — Rendimiento de la carga\n\n"
                + "| Medida | Valor |\n| --- | --- |\n"
                + "| Archivo | " + RNF1_CONTRATOS + " contratos ficticios (" + csv.length / 1024 + " KB, semilla 2026) |\n"
                + "| Alertas que produce | " + alertas + " |\n"
                + "| Corridas medidas | " + RNF1_CORRIDAS + " (después de 1 de calentamiento), base vacía en cada una |\n"
                + "| Tiempos (ms) | " + tiempos + " |\n"
                + "| Mejor / peor (ms) | " + mejor + " / **" + peor + "** |\n"
                + "| Objetivo | peor ≤ " + RNF1_LIMITE_MS + " ms |\n"
                + "| Resultado | " + (peor <= RNF1_LIMITE_MS ? "✅ Cumple" : "❌ No cumple") + " |\n");

        assertTrue(peor <= RNF1_LIMITE_MS, "RNF-1: la carga más lenta tardó " + peor + " ms");
    }

    @Test
    @DisplayName("RNF-2 integridad: 10 recargas del mismo archivo no duplican nada")
    void rnf2_integridad_ante_recargas() throws Exception {
        byte[] csv = generarCsv(RNF2_CONTRATOS, 7);

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

        INFORME.add("## RNF-2 — Integridad ante recargas (idempotencia)\n\n"
                + "| Medida | Valor |\n| --- | --- |\n"
                + "| Archivo | " + RNF2_CONTRATOS + " contratos ficticios (semilla 7) |\n"
                + "| Tras la 1.ª carga | " + contratos + " contratos, " + alertas + " alertas, "
                + evidencia + " filas de evidencia |\n"
                + "| Recargas del mismo archivo | " + RNF2_RECARGAS + " |\n"
                + "| Contratos nuevos en las recargas | " + nuevosEnRecargas + " |\n"
                + "| Recargas sin ningún duplicado | **" + recargasSinCambios + " de " + RNF2_RECARGAS + "** |\n"
                + "| Objetivo | " + RNF2_RECARGAS + " de " + RNF2_RECARGAS + ", 0 contratos nuevos |\n"
                + "| Resultado | " + (recargasSinCambios == RNF2_RECARGAS && nuevosEnRecargas == 0
                        ? "✅ Cumple" : "❌ No cumple") + " |\n");

        assertEquals(0, nuevosEnRecargas, "RNF-2: una recarga creó contratos nuevos");
        assertEquals(RNF2_RECARGAS, recargasSinCambios, "RNF-2: alguna recarga duplicó datos");
    }

    @AfterAll
    static void escribirInforme() throws Exception {
        Runtime rt = Runtime.getRuntime();
        String entorno = "Medido el " + OffsetDateTime.now() + " · Java " + System.getProperty("java.version")
                + " · " + System.getProperty("os.name") + " · " + rt.availableProcessors() + " núcleos · "
                + rt.maxMemory() / (1024 * 1024) + " MB de memoria para la JVM · PostgreSQL 16 en contenedor\n\n"
                + "Método: `RequisitosNoFuncionalesTest` envía el CSV por HTTP real (`POST /api/contratos/cargar`) "
                + "y mide el tiempo desde que se envía hasta que llega la respuesta completa.\n";
        Path salida = Path.of("target", "rnf", "resultados.md");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, "# Medición de requisitos no funcionales\n\n" + entorno + "\n"
                + String.join("\n", INFORME), StandardCharsets.UTF_8);
        System.out.println(Files.readString(salida));
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
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }
}
