package dac;

import dac.dominio.DetectorDeReincidencias;
import dac.dominio.FilaDescartada;
import dac.dominio.ResultadoCarga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dac.CargadorDeContratosTest.ENCABEZADO;
import static dac.CargadorDeContratosTest.carga;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-04 — No generar alertas a partir de filas incompletas.
 *
 * <p>Implementa la Decisión 12: la fila incompleta se descarta y se reporta;
 * el archivo sigue siendo válido. Antes de esta historia, dos filas vacías
 * producían una alerta del par ("", ""): una alerta sobre nadie (R-6).
 */
class FilasIncompletasTest {

    @Test
    @DisplayName("criterio 1: dos filas sin contratista ni funcionario no generan alerta")
    void dos_filas_vacias_no_generan_alerta() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,,,100,2026-01-01\n"
                + "002,Alcaldía,,,200,2026-01-02\n");

        assertEquals(0, resultado.contratos().size(), "ninguna de las dos filas es utilizable");
        assertTrue(new DetectorDeReincidencias().detectar(resultado.contratos()).isEmpty(),
                "ya no se genera la alerta del par vacío (R-6)");
    }

    @Test
    @DisplayName("criterio 2: informa cuántas filas se descartaron y por qué")
    void informa_cuantas_y_por_que() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía,,Ana Ruiz,200,2026-01-02\n"
                + "003,Alcaldía,,,300,2026-01-03\n");

        assertEquals(2, resultado.cantidadDescartadas(), "dos filas incompletas");

        FilaDescartada primera = resultado.filasDescartadas().get(0);
        assertEquals(3, primera.fila(), "la fila 1 es el encabezado, así que esta es la 3");
        assertTrue(primera.motivo().contains("contratista"),
                "el motivo debe nombrar el campo que falta: " + primera.motivo());

        FilaDescartada segunda = resultado.filasDescartadas().get(1);
        assertTrue(segunda.motivo().contains("contratista") && segunda.motivo().contains("funcionario"),
                "cuando faltan dos campos, el motivo nombra los dos: " + segunda.motivo());
    }

    @Test
    @DisplayName("una fila incompleta no invalida el archivo (Decisión 12)")
    void una_fila_mala_no_invalida_el_archivo() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "002,Alcaldía,,,200,2026-01-02\n"
                + "003,Gobernación,ACME SAS,Juan Pérez,300,2026-01-03\n");

        assertEquals(2, resultado.contratos().size(), "las filas buenas se cargan igual");
        assertEquals(1, new DetectorDeReincidencias().detectar(resultado.contratos()).size(),
                "y la reincidencia real se sigue detectando");
    }

    @Test
    @DisplayName("una celda con solo espacios cuenta como vacía")
    void solo_espacios_cuenta_como_vacio() {
        ResultadoCarga resultado = carga(ENCABEZADO + "001,Alcaldía,   ,Juan Pérez,100,2026-01-01\n");

        assertEquals(0, resultado.contratos().size(), "un espacio no es un contratista");
        assertEquals(1, resultado.cantidadDescartadas(), "y se reporta como descartada");
    }

    @Test
    @DisplayName("una fila sin clave natural se descarta: no se puede deduplicar (R-2)")
    void sin_clave_natural_se_descarta() {
        ResultadoCarga resultado = carga(ENCABEZADO + ",Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n");

        assertEquals(0, resultado.contratos().size(), "sin número de contrato no hay identidad");
        assertTrue(resultado.filasDescartadas().get(0).motivo().contains("numero_contrato"),
                "el motivo nombra el campo faltante");
    }

    @Test
    @DisplayName("monto y fecha vacíos NO descartan la fila (R-6, R-8)")
    void monto_y_fecha_vacios_no_descartan() {
        ResultadoCarga resultado = carga(ENCABEZADO + "001,Alcaldía,ACME SAS,Juan Pérez,,\n");

        assertEquals(1, resultado.contratos().size(),
                "la señal no usa monto ni fecha: descartar la fila perdería una reincidencia real");
        assertEquals(0, resultado.cantidadDescartadas(), "no hay nada que reportar");
    }

    @Test
    @DisplayName("una línea en blanco no se reporta como fila descartada")
    void linea_en_blanco_no_se_reporta() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "\n");

        assertEquals(1, resultado.contratos().size(), "el contrato bueno se carga");
        assertEquals(0, resultado.cantidadDescartadas(),
                "una línea vacía no es un problema de calidad de datos");
    }
}
