package dac;

import dac.dominio.ConflictoDeDuplicado;
import dac.dominio.ResultadoCarga;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dac.CargadorDeContratosTest.ENCABEZADO;
import static dac.CargadorDeContratosTest.carga;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-06 — Saber cuándo un duplicado traía datos distintos. Antes ganaba la
 * primera fila sin avisar (problema duro §4); ahora sigue ganando la primera
 * (Decisión 6), pero el conflicto queda reportado.
 */
class ConflictosDeDuplicadosTest {

    @Test
    @DisplayName("criterio 1: misma clave con montos distintos se carga una vez y reporta el conflicto")
    void monto_distinto_se_reporta() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "001,Alcaldía,ACME SAS,Juan Pérez,999,2026-01-01\n");

        assertEquals(1, resultado.contratos().size(), "el contrato se carga una sola vez");
        assertEquals("100", resultado.contratos().get(0).monto(), "gana la primera versión (Decisión 6)");
        assertEquals(1, resultado.conflictos().size(), "el conflicto no se pierde en silencio");

        ConflictoDeDuplicado conflicto = resultado.conflictos().get(0);
        assertEquals(3, conflicto.fila(), "fila que traía la versión distinta");
        assertEquals("fila 2", conflicto.conflictoCon(), "versión con la que choca");
        assertEquals(java.util.List.of("monto"), conflicto.camposDistintos());
    }

    @Test
    @DisplayName("criterio 2: un duplicado idéntico no es un conflicto")
    void duplicado_identico_no_es_conflicto() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n");

        assertEquals(1, resultado.duplicadosIgnorados());
        assertTrue(resultado.conflictos().isEmpty(), "nada que decidir: los datos son iguales");
    }

    @Test
    @DisplayName("criterio 3: el conflicto nombra todos los campos que difieren")
    void nombra_todos_los_campos() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "001,Alcaldía,ACME SAS,Ana Ruiz,100,2026-02-01\n");

        assertEquals(java.util.List.of("funcionario", "fecha"),
                resultado.conflictos().get(0).camposDistintos());
    }

    @Test
    @DisplayName("misma número en otra entidad no es duplicado ni conflicto (R-2)")
    void otra_entidad_no_es_conflicto() {
        ResultadoCarga resultado = carga(ENCABEZADO
                + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                + "001,Gobernación,ACME SAS,Juan Pérez,999,2026-01-01\n");

        assertEquals(2, resultado.contratos().size());
        assertTrue(resultado.conflictos().isEmpty());
    }
}
