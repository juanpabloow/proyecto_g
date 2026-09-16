package dac.aplicacion;

import dac.dominio.Alerta;
import dac.dominio.FilaDescartada;

import java.util.List;

/**
 * Lo que responde la rebanada por HTTP. Los cuatro primeros campos son las
 * dos salidas de la carga (lo que entró y lo que no) más el estado de la
 * base; {@code alertas} es la señal del MVP sobre el histórico completo.
 */
public record ResultadoServicio(
        int contratosNuevos,
        int totalEnBd,
        int duplicadosIgnorados,
        List<FilaDescartada> filasDescartadas,
        List<Alerta> alertas) {
}
