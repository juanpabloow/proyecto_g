package dac;

import dac.dominio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-08 — Exportar el reporte de alertas. */
class ExportadorDeAlertasTest {

    private static final Contrato C1 = Contrato.de("001", "Alcaldía", "ACME SAS", "Juan Pérez", "50000000", "2026-01-15");
    private static final Contrato C2 = Contrato.de("003", "Gobernación", "ACME SAS", "Juan Pérez", "45000000", "2026-03-05");

    private static String[] lineas(String csv) {
        return csv.split("\r\n");
    }

    @Test
    @DisplayName("criterio 1: una fila por contrato de evidencia, con encabezado")
    void una_fila_por_evidencia() {
        String csv = ExportadorDeAlertas.exportar(List.of(new Alerta("ACME SAS", "Juan Pérez", List.of(C1, C2))), List.of());

        String[] lineas = lineas(csv);
        assertEquals(3, lineas.length, "encabezado + 2 contratos de evidencia");
        assertTrue(lineas[0].endsWith(ExportadorDeAlertas.ENCABEZADO), lineas[0]);
        assertTrue(lineas[1].startsWith("REINCIDENCIA;1;ACME SAS;Juan Pérez;Alcaldía;001;50000000;2026-01-15"), lineas[1]);
    }

    @Test
    @DisplayName("criterio 2: incluye también las alertas de fraccionamiento con su detalle")
    void incluye_fraccionamiento() {
        AlertaFraccionamiento f = new AlertaFraccionamiento("ACME SAS", "Gobernación", List.of(C1, C2),
                new BigDecimal("95000000"), new BigDecimal("90000000"), 30);

        String csv = ExportadorDeAlertas.exportar(List.of(), List.of(f));

        assertTrue(csv.contains("FRACCIONAMIENTO;1;"), csv);
        assertTrue(csv.contains("suma 95000000 >= tope 90000000 en 30 días"), csv);
    }

    @Test
    @DisplayName("criterio 3: sin alertas, el archivo trae solo el encabezado")
    void sin_alertas_solo_encabezado() {
        assertEquals(1, lineas(ExportadorDeAlertas.exportar(List.of(), List.of())).length);
    }

    @Test
    @DisplayName("criterio 4: empieza con BOM para que Excel lea bien las tildes")
    void trae_bom() {
        assertEquals('﻿', ExportadorDeAlertas.exportar(List.of(), List.of()).charAt(0));
    }

    @Test
    @DisplayName("seguridad: una celda que parece fórmula se neutraliza (inyección en CSV)")
    void neutraliza_formulas() {
        Contrato malicioso = Contrato.de("002", "Alcaldía", "=HYPERLINK(\"http://x\")", "Juan Pérez", "1", "2026-01-16");
        String csv = ExportadorDeAlertas.exportar(
                List.of(new Alerta("=HYPERLINK(\"http://x\")", "Juan Pérez", List.of(C1, malicioso))), List.of());

        assertFalse(csv.contains(";=HYPERLINK"), "la fórmula no debe quedar ejecutable: " + csv);
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://x\"\")\""), csv);
    }

    @Test
    @DisplayName("un valor con el separador va entre comillas")
    void separador_entre_comillas() {
        Contrato conPuntoYComa = Contrato.de("004", "Alcaldía", "ACME; SAS", "Juan Pérez", "1", "2026-01-16");
        String csv = ExportadorDeAlertas.exportar(
                List.of(new Alerta("ACME; SAS", "Juan Pérez", List.of(conPuntoYComa, C1))), List.of());

        assertTrue(csv.contains(";\"ACME; SAS\";"), csv);
    }
}
