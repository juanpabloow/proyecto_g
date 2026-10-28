package dac.dominio;

import java.util.List;
import java.util.Map;

public record ResultadoCarga(
        List<Contrato> contratos,
        List<FilaDescartada> filasDescartadas,
        int duplicadosIgnorados,
        List<ConflictoDeDuplicado> conflictos,
        Map<String, Integer> filaPorClave) {

    public ResultadoCarga {
        contratos = List.copyOf(contratos);
        filasDescartadas = List.copyOf(filasDescartadas);
        conflictos = List.copyOf(conflictos);
        filaPorClave = Map.copyOf(filaPorClave);
    }

    public ResultadoCarga(List<Contrato> contratos, List<FilaDescartada> filasDescartadas,
                          int duplicadosIgnorados) {
        this(contratos, filasDescartadas, duplicadosIgnorados, List.of(), Map.of());
    }

    public int cantidadDescartadas() {
        return filasDescartadas.size();
    }

    /** Fila del archivo de la que salió el contrato (0 si no se conoce). */
    public int filaDe(Contrato contrato) {
        return filaPorClave.getOrDefault(contrato.claveNatural(), 0);
    }
}
