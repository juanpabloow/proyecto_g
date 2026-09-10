package dac.dominio;

import java.util.List;

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
}
