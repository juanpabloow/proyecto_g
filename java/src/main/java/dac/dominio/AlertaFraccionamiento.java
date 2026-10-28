package dac.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * HU-03 — Un contratista con varios contratos pequeños en la misma entidad,
 * en pocos días, que juntos superan el tope de cuantía. Como toda alerta de
 * DAC, describe una coincidencia, no una culpa (R-4), y trae su evidencia.
 */
public record AlertaFraccionamiento(
        String contratista,
        String entidad,
        List<Contrato> evidencia,
        BigDecimal montoTotal,
        BigDecimal tope,
        int ventanaDias) {

    public AlertaFraccionamiento {
        evidencia = List.copyOf(evidencia);
        if (evidencia.size() < 2) {
            throw new IllegalArgumentException(
                    "Un fraccionamiento necesita al menos 2 contratos de evidencia (HU-03). Contratista: "
                    + contratista);
        }
    }

    public String tipo() {
        return "FRACCIONAMIENTO";
    }
}
