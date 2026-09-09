package dac;

/**
 * Una fila que no se convirtió en contrato, y el motivo.
 *
 * <p>Existe porque descartar en silencio esconde un problema de calidad de
 * datos: el analista tiene que enterarse de qué se quedó afuera
 * (HU-04, criterio 2).
 *
 * @param fila   número de fila dentro del CSV, donde la fila 1 es el encabezado
 * @param motivo qué le faltaba, en palabras
 */
public record FilaDescartada(int fila, String motivo) {

    @Override
    public String toString() {
        return "fila " + fila + ": " + motivo;
    }
}
