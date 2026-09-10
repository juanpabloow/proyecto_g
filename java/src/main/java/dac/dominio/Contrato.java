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

    /** El par (contratista, funcionario): la unidad sobre la que se alerta (R-3). */
    public String par() {
        return contratista + SEPARADOR + funcionario;
    }
}
