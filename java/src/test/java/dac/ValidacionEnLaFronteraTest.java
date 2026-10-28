package dac;

import dac.dominio.ResultadoCarga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dac.CargadorDeContratosTest.ENCABEZADO;
import static dac.CargadorDeContratosTest.carga;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Módulo 7, S-02 — Validación de entradas en la frontera, sin base de datos.
 * Un valor más largo que su columna se descarta con su motivo en lugar de
 * llegar a PostgreSQL y romper la carga completa.
 */
class ValidacionEnLaFronteraTest {

    @Test
    @DisplayName("S-02: un contratista de más de 200 caracteres se descarta con su motivo")
    void nombre_demasiado_largo_se_descarta() {
        String largo = "A".repeat(201);
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía," + largo + ",Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-02\n");

        assertEquals(1, resultado.contratos().size(), "la fila válida sí se carga");
        assertEquals(1, resultado.cantidadDescartadas());
        assertTrue(resultado.filasDescartadas().get(0).motivo().contains("contratista supera 200"),
                resultado.filasDescartadas().get(0).motivo());
    }

    @Test
    @DisplayName("S-02: un número de contrato de más de 50 caracteres se descarta")
    void numero_demasiado_largo_se_descarta() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "1".repeat(51) + ",Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n");

        assertEquals(0, resultado.contratos().size());
        assertTrue(resultado.filasDescartadas().get(0).motivo().contains("numero_contrato supera 50"));
    }

    @Test
    @DisplayName("S-02: justo en el límite (200 caracteres) sí se acepta")
    void en_el_limite_se_acepta() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía," + "A".repeat(200) + ",Juan Pérez,100,2026-01-01\n");

        assertEquals(1, resultado.contratos().size());
    }
}
