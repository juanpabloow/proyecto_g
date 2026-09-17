package dac;

import dac.dominio.Alerta;
import dac.dominio.Contrato;
import dac.dominio.DetectorDeReincidencias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-02 — Detectar reincidencia contratista-funcionario (historia central).
 * Un caso por cada criterio de aceptación de la historia.
 */
class DetectorDeReincidenciasTest {

    private static Contrato contrato(String numero, String contratista, String funcionario) {
        return Contrato.de(numero, "Alcaldía", contratista, funcionario, "100", "2026-01-01");
    }

    private static List<Alerta> detectar(List<Contrato> contratos) {
        return new DetectorDeReincidencias().detectar(contratos);
    }

    @Test
    @DisplayName("criterio 1: un par repetido genera una alerta")
    void un_par_repetido_genera_alerta() {
        List<Alerta> alertas = detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME SAS", "Juan Pérez")));

        assertEquals(1, alertas.size(), "el par se repite: debe haber una alerta");
        assertEquals("ACME SAS", alertas.get(0).contratista(), "contratista señalado");
        assertEquals("Juan Pérez", alertas.get(0).funcionario(), "funcionario señalado");
    }

    @Test
    @DisplayName("criterio 2: sin repeticiones no hay alertas")
    void sin_repeticiones_no_hay_alertas() {
        assertTrue(detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "Otra Ltda", "Ana Ruiz"))).isEmpty(),
                "ningún par se repite");
    }

    @Test
    @DisplayName("criterio 3: un par en 3 contratos da una sola alerta con 3 evidencias")
    void tres_contratos_una_sola_alerta() {
        List<Alerta> alertas = detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME SAS", "Juan Pérez"),
                contrato("003", "ACME SAS", "Juan Pérez")));

        assertEquals(1, alertas.size(), "una alerta por par, no una por contrato");
        assertEquals(3, alertas.get(0).cantidadContratos(), "los tres contratos son la evidencia");
    }

    @Test
    @DisplayName("criterio 4: mismo contratista con funcionarios distintos no alerta")
    void la_senal_es_el_par_no_el_contratista() {
        assertTrue(detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME SAS", "Ana Ruiz"))).isEmpty(),
                "la señal es el par, no el contratista por sí solo");
    }

    @Test
    @DisplayName("criterio 5: toda alerta trae al menos 2 contratos de evidencia")
    void toda_alerta_trae_evidencia() {
        List<Alerta> alertas = detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME SAS", "Juan Pérez"),
                contrato("003", "Otra Ltda", "Ana Ruiz"),
                contrato("004", "Constructora XYZ", "Carlos Gómez"),
                contrato("005", "Constructora XYZ", "Carlos Gómez")));

        assertEquals(2, alertas.size(), "dos pares reincidentes");
        alertas.forEach(a -> assertTrue(a.cantidadContratos() >= 2,
                "una alerta sin evidencia es un defecto: " + a.contratista()));
    }

    @Test
    @DisplayName("criterio 5: construir una alerta con un solo contrato es un error")
    void una_alerta_con_un_contrato_es_un_defecto() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> new Alerta("ACME SAS", "Juan Pérez",
                        List.of(contrato("001", "ACME SAS", "Juan Pérez"))));

        assertTrue(error.getMessage().contains("defecto"), error.getMessage());
    }

    @Test
    @DisplayName("criterio 7: la evidencia trae número, entidad, fecha y monto de cada contrato")
    void la_evidencia_trae_los_datos_del_contrato() {
        Alerta alerta = detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME SAS", "Juan Pérez"))).get(0);

        Contrato evidencia = alerta.evidencia().get(0);

        assertEquals("001", evidencia.numeroContrato(), "falta el número de contrato");
        assertEquals("Alcaldía", evidencia.entidad(), "falta la entidad");
        assertEquals("2026-01-01", evidencia.fecha(), "falta la fecha");
        assertEquals("100", evidencia.monto(), "falta el monto");
    }

    @Test
    @DisplayName("limitación conocida: nombres escritos distinto no se unen (R-5, HU-05)")
    void nombres_escritos_distinto_no_se_unen() {
        assertTrue(detectar(List.of(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "acme sas", "juan pérez"))).isEmpty(),
                "hoy son contratistas distintos: falso negativo documentado, no un descuido");
    }
}
