package dac;

import java.util.List;

/**
 * El par señalado más los contratos que lo originaron.
 *
 * <p>La evidencia no es un extra: una alerta sin los contratos que la
 * sustentan obliga al analista a rehacer el cruce a mano, que es
 * exactamente lo que DAC viene a evitar (HU-02).
 *
 * <p>Una alerta describe una coincidencia, no una culpa (R-4).
 */
public record Alerta(String contratista, String funcionario, List<Contrato> evidencia) {

    public Alerta {
        evidencia = List.copyOf(evidencia);
        if (evidencia.size() < 2) {
            throw new IllegalArgumentException(
                    "Una alerta con menos de 2 contratos de evidencia es un defecto, no una alerta "
                    + "(HU-02, criterio 5). Par señalado: " + contratista + " + " + funcionario);
        }
    }

    public int cantidadContratos() {
        return evidencia.size();
    }

    /**
     * La alerta en texto. Muestra por contrato su número, entidad, fecha y
     * monto: lo mínimo para ir a buscar el expediente (HU-02, criterio 7).
     */
    public String mostrar() {
        StringBuilder texto = new StringBuilder();
        texto.append("[!] ").append(contratista).append(" + ").append(funcionario).append('\n');
        for (Contrato contrato : evidencia) {
            texto.append("   - Contrato ").append(contrato.numeroContrato())
                 .append(" (").append(contrato.entidad())
                 .append(", ").append(contrato.fecha())
                 .append(", $").append(contrato.monto()).append(")\n");
        }
        return texto.toString();
    }
}
