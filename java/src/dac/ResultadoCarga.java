package dac;

import java.util.List;

/**
 * Lo que devuelve una carga: los contratos utilizables y lo que quedó afuera.
 *
 * <p>Es un resultado y no una simple lista de contratos porque, desde HU-04,
 * la carga tiene dos salidas y las dos importan. Devolver solo la primera
 * obligaría a descartar en silencio.
 */
public record ResultadoCarga(
        List<Contrato> contratos,
        List<FilaDescartada> filasDescartadas,
        int duplicadosIgnorados) {

    public ResultadoCarga {
        contratos = List.copyOf(contratos);
        filasDescartadas = List.copyOf(filasDescartadas);
    }

    public int cantidadDescartadas() {
        return filasDescartadas.size();
    }

    /** El informe de la carga, tal como lo lee el analista (HU-04, criterio 2). */
    public String resumen() {
        StringBuilder texto = new StringBuilder();
        texto.append("Contratos cargados: ").append(contratos.size());

        if (duplicadosIgnorados > 0) {
            texto.append('\n').append("Duplicados ignorados: ").append(duplicadosIgnorados)
                 .append(" (mismo número de contrato en la misma entidad — R-2)");
        }

        texto.append('\n').append("Filas descartadas: ").append(filasDescartadas.size());
        if (!filasDescartadas.isEmpty()) {
            texto.append(" (el resto del archivo se analiza igual — Decisión 12)");
            for (FilaDescartada descartada : filasDescartadas) {
                texto.append('\n').append("   - ").append(descartada);
            }
        }
        return texto.toString();
    }
}
