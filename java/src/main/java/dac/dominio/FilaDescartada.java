package dac.dominio;

/** Una fila que no se convirtió en contrato, y el motivo (HU-04). */
public record FilaDescartada(int fila, String motivo) {

    @Override
    public String toString() {
        return "fila " + fila + ": " + motivo;
    }
}
