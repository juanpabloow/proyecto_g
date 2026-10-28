package dac.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Módulo 7, S-07 — El registro de auditoría no se puede falsificar desde un dato. */
class AuditoriaTest {

    @Test
    @DisplayName("S-07 inyección en logs: un salto de línea en un dato no crea una línea nueva")
    void quita_saltos_de_linea() {
        String limpio = Auditoria.limpio("datos.csv\nAUDITORIA carga aceptada archivo=\"falso\"");

        assertFalse(limpio.contains("\n"), limpio);
        assertFalse(limpio.contains("\r"), limpio);
    }

    @Test
    @DisplayName("S-07 un texto muy largo se recorta a 200 caracteres")
    void recorta_textos_largos() {
        assertEquals(201, Auditoria.limpio("x".repeat(500)).length(), "200 caracteres + el signo …");
    }

    @Test
    @DisplayName("S-07 un dato nulo no rompe el registro")
    void nulo() {
        assertEquals("", Auditoria.limpio(null));
    }
}
