package dac.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fuera del perfil de desarrollo no hay credencial por defecto (Módulo 7):
 * si faltan DAC_AUTH_USER o DAC_AUTH_PASSWORD la aplicación no arranca y el
 * mensaje dice qué hacer.
 */
class CredencialDelAnalistaTest {

    @Test
    @DisplayName("sin clave configurada la aplicación no arranca y dice qué variable falta")
    void sin_clave_no_arranca() {
        var error = assertThrows(IllegalStateException.class,
                () -> SeguridadWeb.exigirCredencial("analista", ""));
        assertTrue(error.getMessage().contains("DAC_AUTH_PASSWORD"), error.getMessage());
        assertTrue(error.getMessage().contains("java/ejecutar.sh dev"), error.getMessage());
    }

    @Test
    @DisplayName("sin usuario configurado tampoco arranca")
    void sin_usuario_no_arranca() {
        assertThrows(IllegalStateException.class, () -> SeguridadWeb.exigirCredencial(" ", "una-clave"));
        assertThrows(IllegalStateException.class, () -> SeguridadWeb.exigirCredencial(null, null));
    }

    @Test
    @DisplayName("con usuario y clave definidos arranca normalmente")
    void con_credencial_arranca() {
        assertDoesNotThrow(() -> SeguridadWeb.exigirCredencial("analista", "clave-del-entorno"));
    }
}
