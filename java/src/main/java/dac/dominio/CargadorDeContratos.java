package dac.dominio;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Carga contratos desde un CSV: valida el archivo, descarta filas incompletas
 * y no cuenta dos veces el mismo contrato (idempotencia — problema duro).
 */
@Component
public final class CargadorDeContratos {

    public static final List<String> COLUMNAS_REQUERIDAS = List.of(
            "numero_contrato", "entidad", "contratista", "funcionario", "monto", "fecha");

    public static final List<String> CAMPOS_OBLIGATORIOS = List.of(
            "numero_contrato", "entidad", "contratista", "funcionario");

    /**
     * Carga desde un InputStream (multipart upload vía REST).
     */
    public ResultadoCarga cargarDesde(InputStream stream) {
        String texto;
        try {
            texto = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el CSV", e);
        }
        if (!texto.isEmpty() && texto.charAt(0) == '\uFEFF') {
            texto = texto.substring(1); // BOM de Excel
        }
        return procesar(texto);
    }

    private ResultadoCarga procesar(String texto) {
        List<List<String>> filas = LectorCSV.filas(texto);

        if (filas.isEmpty() || LectorCSV.estaEnBlanco(filas.get(0))) {
            throw new IllegalArgumentException("El CSV está vacío o no tiene encabezado");
        }

        List<String> encabezado = filas.get(0).stream().map(String::strip).toList();
        validarColumnas(encabezado);

        List<Contrato> contratos = new ArrayList<>();
        List<FilaDescartada> descartadas = new ArrayList<>();
        Set<String> clavesVistas = new HashSet<>();
        int duplicados = 0;

        for (int i = 1; i < filas.size(); i++) {
            List<String> valores = filas.get(i);
            if (esLineaVacia(valores)) continue;

            Map<String, String> fila = aMapa(encabezado, valores);
            List<String> faltantes = camposVacios(fila);

            if (!faltantes.isEmpty()) {
                descartadas.add(new FilaDescartada(i + 1, motivo(faltantes)));
                continue;
            }

            Contrato contrato = aContrato(fila, encabezado);
            if (!clavesVistas.add(contrato.claveNatural())) {
                duplicados++;
                continue;
            }
            contratos.add(contrato);
        }
        return new ResultadoCarga(contratos, descartadas, duplicados);
    }

    private static void validarColumnas(List<String> encabezado) {
        List<String> faltantes = COLUMNAS_REQUERIDAS.stream()
                .filter(c -> !encabezado.contains(c)).toList();
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException("Faltan columnas en el CSV: " + faltantes);
        }
    }

    private static boolean esLineaVacia(List<String> valores) {
        return valores.size() <= 1 && LectorCSV.estaEnBlanco(valores);
    }

    private static Map<String, String> aMapa(List<String> encabezado, List<String> valores) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < encabezado.size(); i++) {
            fila.put(encabezado.get(i), i < valores.size() ? valores.get(i) : "");
        }
        return fila;
    }

    private static List<String> camposVacios(Map<String, String> fila) {
        return CAMPOS_OBLIGATORIOS.stream()
                .filter(c -> fila.getOrDefault(c, "").isBlank()).toList();
    }

    private static String motivo(List<String> faltantes) {
        String lista = String.join(", ", faltantes);
        return faltantes.size() == 1 ? "falta el campo " + lista : "faltan los campos " + lista;
    }

    private static Contrato aContrato(Map<String, String> fila, List<String> encabezado) {
        Map<String, String> adicionales = new LinkedHashMap<>();
        for (String col : encabezado) {
            if (!COLUMNAS_REQUERIDAS.contains(col)) adicionales.put(col, fila.get(col));
        }
        return new Contrato(fila.get("numero_contrato"), fila.get("entidad"),
                fila.get("contratista"), fila.get("funcionario"),
                fila.get("monto"), fila.get("fecha"), adicionales);
    }
}
