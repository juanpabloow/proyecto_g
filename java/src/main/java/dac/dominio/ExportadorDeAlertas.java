package dac.dominio;

import java.util.List;

/**
 * HU-08 — Exportar el reporte de alertas a un archivo CSV que se abre en
 * Excel: una fila por cada contrato de evidencia, para que el equipo de
 * investigación (S-3) pueda buscar cada expediente.
 *
 * <p>Seguridad: una celda que empieza por {@code = + - @} se exporta con un
 * apóstrofo delante, para que Excel no la ejecute como fórmula (inyección de
 * fórmulas en CSV). Los nombres vienen de archivos externos y no son
 * confiables.
 */
public final class ExportadorDeAlertas {

    public static final char SEPARADOR = ';';
    public static final String ENCABEZADO = String.join(String.valueOf(SEPARADOR),
            "tipo", "alerta", "contratista", "funcionario", "entidad",
            "numero_contrato", "monto", "fecha", "detalle");

    private ExportadorDeAlertas() {}

    public static String exportar(List<Alerta> reincidencias, List<AlertaFraccionamiento> fraccionamientos) {
        StringBuilder csv = new StringBuilder("﻿").append(ENCABEZADO).append("\r\n");
        int numero = 1;
        for (Alerta a : reincidencias) {
            String detalle = a.cantidadContratos() + " contratos del mismo par";
            for (Contrato c : a.evidencia()) {
                fila(csv, "REINCIDENCIA", numero, c, detalle);
            }
            numero++;
        }
        for (AlertaFraccionamiento a : fraccionamientos) {
            String detalle = "suma " + a.montoTotal().toPlainString() + " >= tope "
                    + a.tope().toPlainString() + " en " + a.ventanaDias() + " días";
            for (Contrato c : a.evidencia()) {
                fila(csv, "FRACCIONAMIENTO", numero, c, detalle);
            }
            numero++;
        }
        return csv.toString();
    }

    private static void fila(StringBuilder csv, String tipo, int numero, Contrato c, String detalle) {
        List<String> celdas = List.of(tipo, String.valueOf(numero), c.contratista(), c.funcionario(),
                c.entidad(), c.numeroContrato(), valor(c.monto()), valor(c.fecha()), detalle);
        for (int i = 0; i < celdas.size(); i++) {
            if (i > 0) csv.append(SEPARADOR);
            csv.append(celda(celdas.get(i)));
        }
        csv.append("\r\n");
    }

    private static String valor(String v) {
        return v == null ? "" : v;
    }

    static String celda(String valor) {
        String v = valor == null ? "" : valor;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !esNumero(v)) {
            v = "'" + v;
        }
        if (v.indexOf(SEPARADOR) >= 0 || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static boolean esNumero(String v) {
        return v.matches("-?\\d+(\\.\\d+)?");
    }
}
