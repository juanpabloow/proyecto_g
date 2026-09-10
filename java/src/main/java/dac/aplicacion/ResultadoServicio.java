package dac.aplicacion;

import dac.dominio.Alerta;
import dac.dominio.FilaDescartada;

import java.util.List;

public record ResultadoServicio(
        int contratosNuevos,
        int totalEnBd,
        List<FilaDescartada> filasDescartadas,
        List<Alerta> alertas) {
}
