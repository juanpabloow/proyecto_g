package dac.aplicacion;

import dac.dominio.Alerta;
import dac.dominio.AlertaFraccionamiento;

import java.util.List;

/** Estado actual de la base: lo que muestra la pantalla al abrirse y lo que se exporta (HU-08). */
public record ResumenDeAlertas(
        int totalEnBd,
        List<Alerta> alertas,
        List<AlertaFraccionamiento> alertasFraccionamiento) {
}
