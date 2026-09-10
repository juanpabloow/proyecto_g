package dac.dominio;

import java.util.List;

/**
 * El par señalado más los contratos que lo originaron (HU-02).
 * Una alerta describe una coincidencia, no una culpa (R-4).
 */
public record Alerta(String contratista, String funcionario, List<Contrato> evidencia) {

    public Alerta {
        evidencia = List.copyOf(evidencia);
        if (evidencia.size() < 2) {
            throw new IllegalArgumentException(
                    "Una alerta con menos de 2 contratos es un defecto (HU-02). Par: "
                    + contratista + " + " + funcionario);
        }
    }

    public int cantidadContratos() {
        return evidencia.size();
    }
}
