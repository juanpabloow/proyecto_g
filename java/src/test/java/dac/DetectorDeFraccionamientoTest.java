package dac;

import dac.dominio.AlertaFraccionamiento;
import dac.dominio.Contrato;
import dac.dominio.DetectorDeFraccionamiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-03 — Detectar fraccionamiento de contratos (R-9, Decisión 20).
 * Tope de prueba: 100 millones; ventana: 30 días.
 */
class DetectorDeFraccionamientoTest {

    private static final DetectorDeFraccionamiento DETECTOR =
            new DetectorDeFraccionamiento(new BigDecimal("100000000"), 30);

    private static Contrato contrato(String numero, String entidad, String contratista,
                                     String monto, String fecha) {
        return Contrato.de(numero, entidad, contratista, "Funcionario " + numero, monto, fecha);
    }

    private static List<AlertaFraccionamiento> detectar(Contrato... contratos) {
        return DETECTOR.detectar(List.of(contratos));
    }

    @Test
    @DisplayName("criterio 1: contratos pequeños del mismo contratista y entidad que suman el tope en la ventana generan alerta")
    void contratos_pequenos_que_suman_el_tope_alertan() {
        List<AlertaFraccionamiento> alertas = detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "40000000", "2026-05-02"),
                contrato("202", "Alcaldía", "Suministros del Sur", "35000000", "2026-05-10"),
                contrato("203", "Alcaldía", "Suministros del Sur", "30000000", "2026-05-20"));

        assertEquals(1, alertas.size(), "una alerta por grupo fraccionado");
        AlertaFraccionamiento alerta = alertas.get(0);
        assertEquals(3, alerta.evidencia().size(), "los tres contratos son la evidencia");
        assertEquals(0, alerta.montoTotal().compareTo(new BigDecimal("105000000")), "suma de la evidencia");
        assertEquals("Alcaldía", alerta.entidad());
    }

    @Test
    @DisplayName("criterio 2: si no suman el tope, no hay alerta")
    void sin_alcanzar_el_tope_no_alerta() {
        assertTrue(detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "40000000", "2026-05-02"),
                contrato("202", "Alcaldía", "Suministros del Sur", "35000000", "2026-05-10")).isEmpty());
    }

    @Test
    @DisplayName("criterio 3: contratos fuera de la ventana de días no se suman")
    void fuera_de_la_ventana_no_se_suman() {
        assertTrue(detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "60000000", "2026-01-01"),
                contrato("202", "Alcaldía", "Suministros del Sur", "60000000", "2026-03-15")).isEmpty(),
                "73 días de diferencia: fuera de la ventana de 30");
    }

    @Test
    @DisplayName("criterio 4: en entidades distintas no es fraccionamiento")
    void entidades_distintas_no_alertan() {
        assertTrue(detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "60000000", "2026-05-02"),
                contrato("202", "Gobernación", "Suministros del Sur", "60000000", "2026-05-10")).isEmpty(),
                "cada entidad tiene su propio proceso de contratación");
    }

    @Test
    @DisplayName("criterio 5: un contrato que ya supera el tope no cuenta como fracción")
    void contrato_grande_no_es_fraccion() {
        assertTrue(detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "150000000", "2026-05-02"),
                contrato("202", "Alcaldía", "Suministros del Sur", "10000000", "2026-05-10")).isEmpty(),
                "el de 150 millones ya pasó por el proceso de mayor cuantía");
    }

    @Test
    @DisplayName("criterio 6: contratos sin monto o sin fecha no participan (R-8)")
    void sin_monto_o_fecha_no_participan() {
        assertTrue(detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "", "2026-05-02"),
                contrato("202", "Alcaldía", "Suministros del Sur", "90000000", ""),
                contrato("203", "Alcaldía", "Suministros del Sur", "60000000", "2026-05-03")).isEmpty());
    }

    @Test
    @DisplayName("HU-05 aplica también aquí: el contratista se compara normalizado")
    void contratista_normalizado() {
        assertEquals(1, detectar(
                contrato("201", "Alcaldía", "SUMINISTROS DEL SUR", "60000000", "2026-05-02"),
                contrato("202", "Alcaldía", "suministros del sur", "60000000", "2026-05-10")).size());
    }

    @Test
    @DisplayName("la misma evidencia no se reporta en dos alertas")
    void sin_alertas_solapadas() {
        List<AlertaFraccionamiento> alertas = detectar(
                contrato("201", "Alcaldía", "Suministros del Sur", "60000000", "2026-05-01"),
                contrato("202", "Alcaldía", "Suministros del Sur", "60000000", "2026-05-05"),
                contrato("203", "Alcaldía", "Suministros del Sur", "60000000", "2026-05-09"));

        assertEquals(1, alertas.size());
        assertEquals(3, alertas.get(0).evidencia().size());
    }

    @Test
    @DisplayName("parámetros inválidos se rechazan al configurar")
    void parametros_invalidos() {
        assertThrows(IllegalArgumentException.class, () -> new DetectorDeFraccionamiento(BigDecimal.ZERO, 30));
        assertThrows(IllegalArgumentException.class, () -> new DetectorDeFraccionamiento(BigDecimal.TEN, 0));
    }
}
