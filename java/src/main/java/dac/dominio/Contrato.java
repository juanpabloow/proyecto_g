package dac.dominio;

import java.util.Map;

public record Contrato(
        String numeroContrato,
        String entidad,
        String contratista,
        String funcionario,
        String monto,
        String fecha,
        Map<String, String> camposAdicionales) {

    private static final String SEPARADOR = String.valueOf((char) 31);

    public Contrato {
        camposAdicionales = camposAdicionales == null ? Map.of() : Map.copyOf(camposAdicionales);
    }

    public static Contrato de(String numeroContrato, String entidad, String contratista,
                              String funcionario, String monto, String fecha) {
        return new Contrato(numeroContrato, entidad, contratista, funcionario, monto, fecha, Map.of());
    }

    /** Lo que identifica un contrato: numero + entidad (R-2). */
    public String claveNatural() {
        return numeroContrato + SEPARADOR + entidad;
    }

    /**
     * El par (contratista, funcionario): la unidad sobre la que se alerta (R-3).
     * Se compara con los nombres normalizados (HU-05, R-5): "ACME SAS" y
     * "acme s.a.s." son el mismo contratista para la señal.
     */
    public String par() {
        return NormalizadorDeNombres.normalizar(contratista) + SEPARADOR
                + NormalizadorDeNombres.normalizar(funcionario);
    }

    /** Contratista normalizado + entidad: la unidad del fraccionamiento (HU-03). */
    public String contratistaEnEntidad() {
        return NormalizadorDeNombres.normalizar(contratista) + SEPARADOR + entidad;
    }
}
