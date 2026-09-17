package dac;

import dac.dominio.CargadorDeContratos;
import dac.dominio.Contrato;
import dac.dominio.DetectorDeReincidencias;
import dac.dominio.ResultadoCarga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-01 (cargar contratos desde CSV) y el problema duro (idempotencia).
 *
 * <p>Pruebas de dominio puro: no levantan Spring ni tocan la base de datos.
 * La idempotencia <em>entre ejecuciones</em> (HU-07) es responsabilidad de
 * la base y se verifica en {@link ContratoE2ETest}.
 */
class CargadorDeContratosTest {

    static final String ENCABEZADO = "numero_contrato,entidad,contratista,funcionario,monto,fecha\n";
    private static final String FILA = "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n";

    static ResultadoCarga carga(String csv) {
        return new CargadorDeContratos()
                .cargarDesde(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Nested
    @DisplayName("HU-01 — Cargar contratos desde un archivo CSV")
    class Carga {

        @Test
        @DisplayName("carga un contrato por cada fila válida")
        void carga_una_fila_por_contrato() {
            ResultadoCarga resultado = carga(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "002,Alcaldía,Otra Ltda,Ana Ruiz,200,2026-01-02\n");

            assertEquals(2, resultado.contratos().size(), "deberían cargarse las dos filas");
            assertEquals("ACME SAS", resultado.contratos().get(0).contratista(), "primer contratista");
        }

        @Test
        @DisplayName("falla nombrando la columna que falta, no con un error genérico (R-1)")
        void falla_nombrando_la_columna_faltante() {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> carga("numero_contrato,entidad,contratista\n001,Alcaldía,ACME SAS\n"));

            assertTrue(error.getMessage().contains("funcionario"),
                    "el error debe nombrar la columna que falta: " + error.getMessage());
        }

        @Test
        @DisplayName("falla con mensaje claro si el archivo está vacío")
        void falla_si_el_archivo_esta_vacio() {
            var error = assertThrows(IllegalArgumentException.class, () -> carga(""));

            assertTrue(error.getMessage().contains("vacío"),
                    "el error debe decir que el archivo está vacío: " + error.getMessage());
        }

        @Test
        @DisplayName("conserva las columnas adicionales del CSV (R-1)")
        void conserva_columnas_adicionales() {
            Contrato contrato = carga(ENCABEZADO.strip() + ",objeto\n"
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01,Suministro de papelería\n")
                    .contratos().get(0);

            assertEquals("Suministro de papelería", contrato.camposAdicionales().get("objeto"),
                    "la columna extra debe conservarse");
        }

        @Test
        @DisplayName("lee campos entre comillas que contienen comas")
        void lee_campos_entre_comillas() {
            Contrato contrato = carga(ENCABEZADO
                    + "001,Alcaldía,\"ACME S.A.S., Sucursal Bogotá\",Juan Pérez,100,2026-01-01\n")
                    .contratos().get(0);

            assertEquals("ACME S.A.S., Sucursal Bogotá", contrato.contratista(),
                    "la coma dentro de comillas no parte el campo");
            assertEquals("Juan Pérez", contrato.funcionario(), "las columnas siguientes no se corren");
        }
    }

    @Nested
    @DisplayName("Problema duro — Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("la misma clave natural repetida se carga una sola vez")
        void deduplica_por_clave_natural() {
            ResultadoCarga resultado = carga(ENCABEZADO + FILA + FILA + FILA);

            assertEquals(1, resultado.contratos().size(), "tres filas iguales son un solo contrato");
            assertEquals(2, resultado.duplicadosIgnorados(), "los dos duplicados quedan reportados");
        }

        @Test
        @DisplayName("un contrato duplicado no genera una alerta falsa")
        void el_duplicado_no_genera_alerta_falsa() {
            ResultadoCarga resultado = carga(ENCABEZADO + FILA + FILA);

            assertTrue(new DetectorDeReincidencias().detectar(resultado.contratos()).isEmpty(),
                    "contar dos veces un contrato no es una reincidencia");
        }

        @Test
        @DisplayName("ante datos distintos con la misma clave, gana el primero (Decisión 6)")
        void gana_el_primero() {
            ResultadoCarga resultado = carga(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "001,Alcaldía,ACME SAS,Juan Pérez,999,2026-01-01\n");

            assertEquals(1, resultado.contratos().size(), "sigue siendo un solo contrato");
            assertEquals("100", resultado.contratos().get(0).monto(),
                    "se conserva el monto de la primera fila");
        }

        @Test
        @DisplayName("el mismo número en entidades distintas son dos contratos (R-2)")
        void el_numero_solo_no_identifica() {
            ResultadoCarga resultado = carga(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "001,Gobernación,ACME SAS,Juan Pérez,200,2026-02-01\n");

            assertEquals(2, resultado.contratos().size(), "el consecutivo 001 se repite entre entidades");
            assertEquals(1, new DetectorDeReincidencias().detectar(resultado.contratos()).size(),
                    "y sí son una reincidencia real: mismo par, dos contratos distintos");
        }

        @Test
        @DisplayName("la deduplicación en memoria es por carga, no por cargador")
        void la_deduplicacion_en_memoria_es_por_carga() {
            CargadorDeContratos cargador = new CargadorDeContratos();

            cargador.cargarDesde(new ByteArrayInputStream((ENCABEZADO + FILA).getBytes(StandardCharsets.UTF_8)));
            ResultadoCarga segunda = cargador.cargarDesde(
                    new ByteArrayInputStream((ENCABEZADO + FILA).getBytes(StandardCharsets.UTF_8)));

            assertEquals(1, segunda.contratos().size(),
                    "el cargador no recuerda cargas anteriores: clavesVistas es local a cada llamada");
            assertEquals(0, segunda.duplicadosIgnorados(),
                    "por eso no lo reporta como duplicado");
            // Quien impide el duplicado real es la base de datos
            // (uq_contrato_clave_natural), y eso lo verifica ContratoE2ETest.
        }
    }
}
