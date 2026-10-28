package dac.aplicacion;

import dac.dominio.Alerta;
import dac.dominio.AlertaFraccionamiento;
import dac.dominio.ConflictoDeDuplicado;
import dac.dominio.FilaDescartada;

import java.util.List;

/**
 * Lo que responde la carga por HTTP: lo que entró, lo que no (descartes y
 * conflictos de HU-06), el estado de la base y las dos señales calculadas
 * sobre el histórico completo (reincidencia, HU-02; fraccionamiento, HU-03).
 */
public record ResultadoServicio(
        int contratosNuevos,
        int totalEnBd,
        int duplicadosIgnorados,
        List<FilaDescartada> filasDescartadas,
        List<ConflictoDeDuplicado> conflictos,
        List<Alerta> alertas,
        List<AlertaFraccionamiento> alertasFraccionamiento) {
}
