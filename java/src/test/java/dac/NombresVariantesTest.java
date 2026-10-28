package dac;

import dac.dominio.Alerta;
import dac.dominio.Contrato;
import dac.dominio.DetectorDeReincidencias;
import dac.dominio.NormalizadorDeNombres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU-05 — Reconocer el mismo nombre escrito de formas distintas (R-5,
 * Decisión 19). Un caso por criterio, más los límites de la normalización:
 * lo que NO debe unirse, porque unirlo señalaría a quien no es (S-6).
 */
class NombresVariantesTest {

    private static Contrato contrato(String numero, String contratista, String funcionario) {
        return Contrato.de(numero, "Alcaldía", contratista, funcionario, "100", "2026-01-01");
    }

    private static List<Alerta> detectar(Contrato... contratos) {
        return new DetectorDeReincidencias().detectar(List.of(contratos));
    }

    @Test
    @DisplayName("criterio 1: distinta capitalización o espacios sobrantes generan la alerta")
    void capitalizacion_y_espacios_no_esconden_la_reincidencia() {
        List<Alerta> alertas = detectar(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "  acme   sas ", " JUAN PÉREZ"));

        assertEquals(1, alertas.size(), "es el mismo par escrito distinto");
        assertEquals(2, alertas.get(0).cantidadContratos());
    }

    @Test
    @DisplayName("criterio 1: tildes y puntos de la razón social no separan al contratista")
    void tildes_y_puntos_no_separan() {
        List<Alerta> alertas = detectar(
                contrato("001", "ACME S.A.S.", "Juan Pérez"),
                contrato("002", "ACME SAS", "Juan Perez"));

        assertEquals(1, alertas.size(), "S.A.S. y SAS, Pérez y Perez son lo mismo");
    }

    @Test
    @DisplayName("criterio 2: la alerta muestra los nombres originales, no los normalizados")
    void la_evidencia_conserva_los_nombres_originales() {
        Alerta alerta = detectar(
                contrato("001", "ACME S.A.S.", "Juan Pérez"),
                contrato("002", "acme sas", "juan pérez")).get(0);

        assertEquals("ACME S.A.S.", alerta.contratista(), "nombre tal como viene en el primer contrato");
        assertEquals("acme sas", alerta.evidencia().get(1).contratista(),
                "cada contrato de evidencia conserva su escritura original");
    }

    @Test
    @DisplayName("límite: sufijos societarios distintos siguen siendo empresas distintas")
    void sufijos_distintos_no_se_unen() {
        assertTrue(detectar(
                contrato("001", "ACME SAS", "Juan Pérez"),
                contrato("002", "ACME LTDA", "Juan Pérez")).isEmpty(),
                "unir SAS con LTDA podría señalar a otra empresa (S-6)");
    }

    @Test
    @DisplayName("límite: la ñ se conserva (Peña y Pena son apellidos distintos)")
    void la_enie_se_conserva() {
        assertNotEquals(NormalizadorDeNombres.normalizar("Ana Peña"),
                NormalizadorDeNombres.normalizar("Ana Pena"));
        assertEquals(NormalizadorDeNombres.normalizar("ANA PEÑA"),
                NormalizadorDeNombres.normalizar("ana peña"), "la ñ mayúscula y minúscula sí son iguales");
    }

    @Test
    @DisplayName("un nombre nulo se normaliza a vacío sin fallar")
    void nombre_nulo() {
        assertEquals("", NormalizadorDeNombres.normalizar(null));
    }
}
