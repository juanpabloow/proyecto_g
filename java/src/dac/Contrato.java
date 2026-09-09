package dac;

import java.util.Map;

/**
 * Un contrato: una fila del CSV ya validada.
 *
 * <p>Los seis campos de R-1 se guardan como texto. {@code monto} y
 * {@code fecha} no se convierten a numero ni a fecha a proposito: la senal
 * del MVP no los usa y convertirlos sin validar reglas seria inventar
 * comportamiento (R-8).
 *
 * <p>Las columnas que el CSV traiga de mas se conservan en
 * {@code camposAdicionales}, como exige R-1.
 */
public record Contrato(
        String numeroContrato,
        String entidad,
        String contratista,
        String funcionario,
        String monto,
        String fecha,
        Map<String, String> camposAdicionales) {

    /**
     * Separador para las claves compuestas: el caracter de control 31
     * ("unit separator"). No aparece en un CSV, asi que dos claves distintas
     * nunca se confunden al concatenarlas.
     */
    private static final String SEPARADOR = String.valueOf((char) 31);

    public Contrato {
        camposAdicionales = camposAdicionales == null ? Map.of() : Map.copyOf(camposAdicionales);
    }

    /** Un contrato sin columnas adicionales. Atajo para pruebas y ejemplos. */
    public static Contrato de(String numeroContrato, String entidad, String contratista,
                              String funcionario, String monto, String fecha) {
        return new Contrato(numeroContrato, entidad, contratista, funcionario, monto, fecha, Map.of());
    }

    /**
     * Lo que identifica un contrato: numero + entidad (R-2).
     *
     * <p>El numero solo no alcanza --dos entidades pueden usar el consecutivo
     * "001"-- y la fila completa tampoco --un monto corregido no lo convierte
     * en otro contrato--. Es el problema duro de esta entrega.
     */
    public String claveNatural() {
        return numeroContrato + SEPARADOR + entidad;
    }

    /** El par (contratista, funcionario): la unidad sobre la que se alerta (R-3). */
    public String par() {
        return contratista + SEPARADOR + funcionario;
    }
}
