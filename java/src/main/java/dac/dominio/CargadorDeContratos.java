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
     * Longitud máxima de cada campo, la misma de las columnas en db/schema.sql.
     * Validar en la frontera (Módulo 7, S-02): un valor más largo descarta la
     * fila con su motivo, en vez de llegar a la base y romper la carga con un 500.
     */
    public static final Map<String, Integer> LONGITUD_MAXIMA = Map.of(
            "numero_contrato", 50, "entidad", 200, "contratista", 200, "funcionario", 200);

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
        Map<String, Integer> filaPorClave = new LinkedHashMap<>();
        Map<String, Contrato> primeraVersion = new HashMap<>();
        List<ConflictoDeDuplicado> conflictos = new ArrayList<>();
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
            String demasiadoLargo = campoDemasiadoLargo(fila);
            if (demasiadoLargo != null) {
                descartadas.add(new FilaDescartada(i + 1, "el campo " + demasiadoLargo
                        + " supera " + LONGITUD_MAXIMA.get(demasiadoLargo) + " caracteres"));
                continue;
            }

            Contrato contrato = aContrato(fila, encabezado);
            Contrato anterior = primeraVersion.putIfAbsent(contrato.claveNatural(), contrato);
            if (anterior != null) {
                duplicados++;
                List<String> distintos = camposDistintos(anterior, contrato);
                if (!distintos.isEmpty()) {
                    conflictos.add(new ConflictoDeDuplicado(i + 1, contrato.numeroContrato(),
                            contrato.entidad(), "fila " + filaPorClave.get(contrato.claveNatural()),
                            distintos));
                }
                continue;
            }
            filaPorClave.put(contrato.claveNatural(), i + 1);
            contratos.add(contrato);
        }
        return new ResultadoCarga(contratos, descartadas, duplicados, conflictos, filaPorClave);
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

    private static String campoDemasiadoLargo(Map<String, String> fila) {
        for (String campo : CAMPOS_OBLIGATORIOS) {
            if (fila.getOrDefault(campo, "").length() > LONGITUD_MAXIMA.get(campo)) return campo;
        }
        return null;
    }

    private static String motivo(List<String> faltantes) {
        String lista = String.join(", ", faltantes);
        return faltantes.size() == 1 ? "falta el campo " + lista : "faltan los campos " + lista;
    }

    /**
     * HU-06: qué campos de un duplicado difieren de la primera versión. Se
     * comparan tal como vienen (sin normalizar): para el analista, "Juan
     * Pérez" y "juan pérez" son la misma persona en la señal, pero una
     * corrección de escritura también es un dato que quiere ver.
     */
    public static List<String> camposDistintos(Contrato primera, Contrato otra) {
        List<String> distintos = new ArrayList<>();
        if (!igual(primera.contratista(), otra.contratista())) distintos.add("contratista");
        if (!igual(primera.funcionario(), otra.funcionario())) distintos.add("funcionario");
        if (!igual(primera.monto(), otra.monto())) distintos.add("monto");
        if (!igual(primera.fecha(), otra.fecha())) distintos.add("fecha");
        return distintos;
    }

    private static boolean igual(String a, String b) {
        return (a == null ? "" : a.strip()).equals(b == null ? "" : b.strip());
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
