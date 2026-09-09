package dac;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Carga contratos desde un CSV: valida el archivo, descarta lo inservible y
 * no cuenta dos veces el mismo contrato.
 *
 * <p><b>Idempotencia (problema duro).</b> {@code clavesVistas} es un campo de
 * instancia, no una variable local: el mismo cargador reconoce un contrato ya
 * cargado aunque venga en otro archivo. Sigue sin sobrevivir al cierre del
 * programa — eso es HU-07 y necesita base de datos.
 *
 * <p><b>Filas incompletas (HU-04, Decisión 12).</b> Una fila sin los campos
 * que la identifican se descarta y se reporta; el resto del archivo se carga
 * igual. Ni se acepta (generaba alertas sobre nadie, R-6) ni tumba el archivo
 * completo.
 */
public final class CargadorDeContratos {

    /** R-1: sin estas seis columnas el archivo no sirve. */
    public static final List<String> COLUMNAS_REQUERIDAS = List.of(
            "numero_contrato", "entidad", "contratista", "funcionario", "monto", "fecha");

    /**
     * Campos sin los cuales la fila se descarta (Decisión 12).
     *
     * <p>{@code contratista} y {@code funcionario} porque son el par sobre el
     * que se alerta: vacíos producen una alerta sobre nadie (HU-04, R-6).
     * {@code numero_contrato} y {@code entidad} porque son la clave natural:
     * sin ella no se puede saber si el contrato ya estaba cargado (R-2).
     *
     * <p>{@code monto} y {@code fecha} vacíos <b>no</b> descartan la fila: no
     * intervienen en la señal (R-8) y perderíamos reincidencias reales por un
     * dato que no usamos.
     */
    public static final List<String> CAMPOS_OBLIGATORIOS = List.of(
            "numero_contrato", "entidad", "contratista", "funcionario");

    private final Set<String> clavesVistas = new HashSet<>();

    public ResultadoCarga cargarDesdeCSV(String ruta) {
        return cargarDesdeCSV(Path.of(ruta));
    }

    public ResultadoCarga cargarDesdeCSV(Path ruta) {
        List<List<String>> filas = LectorCSV.filas(leer(ruta));

        if (filas.isEmpty() || LectorCSV.estaEnBlanco(filas.get(0))) {
            throw new IllegalArgumentException("El CSV está vacío o no tiene encabezado: " + ruta);
        }

        List<String> encabezado = filas.get(0).stream().map(String::strip).toList();
        validarColumnas(encabezado, ruta);

        List<Contrato> contratos = new ArrayList<>();
        List<FilaDescartada> descartadas = new ArrayList<>();
        int duplicados = 0;

        for (int indice = 1; indice < filas.size(); indice++) {
            List<String> valores = filas.get(indice);
            int numeroDeFila = indice + 1;

            if (esLineaVacia(valores)) {
                continue;
            }

            Map<String, String> fila = aMapa(encabezado, valores);

            List<String> faltantes = camposVacios(fila);
            if (!faltantes.isEmpty()) {
                descartadas.add(new FilaDescartada(numeroDeFila, motivo(faltantes)));
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

    private static String leer(Path ruta) {
        String texto;
        try {
            texto = Files.readString(ruta, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer el CSV: " + ruta, error);
        }
        // Excel antepone una marca invisible (BOM) que rompería el primer encabezado.
        boolean tieneBOM = !texto.isEmpty() && texto.charAt(0) == '\uFEFF';
        return tieneBOM ? texto.substring(1) : texto;
    }

    /** R-1: el error nombra las columnas que faltan, no dice solo "error". */
    private static void validarColumnas(List<String> encabezado, Path ruta) {
        List<String> faltantes = COLUMNAS_REQUERIDAS.stream()
                .filter(columna -> !encabezado.contains(columna))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Faltan columnas en el CSV: " + faltantes + " (archivo: " + ruta + ")");
        }
    }

    /** Una línea realmente vacía, no una fila con campos vacíos: se salta sin reportar. */
    private static boolean esLineaVacia(List<String> valores) {
        return valores.size() <= 1 && LectorCSV.estaEnBlanco(valores);
    }

    private static Map<String, String> aMapa(List<String> encabezado, List<String> valores) {
        Map<String, String> fila = new LinkedHashMap<>();
        for (int i = 0; i < encabezado.size(); i++) {
            // Una fila con menos campos que el encabezado deja los últimos vacíos:
            // si son obligatorios, se descarta más adelante con su motivo.
            fila.put(encabezado.get(i), i < valores.size() ? valores.get(i) : "");
        }
        return fila;
    }

    /**
     * Los campos obligatorios que vienen vacíos.
     *
     * <p>Vacío incluye "solo espacios": una celda con un espacio es tan
     * inservible como una vacía. Ojo: eso <b>no</b> es normalizar nombres —
     * los valores se guardan tal como vienen, porque decidir que
     * {@code " Juan Pérez"} y {@code "Juan Pérez"} son la misma persona es
     * HU-05 y necesita validación de S-1 (R-5, Decisión 9).
     */
    private static List<String> camposVacios(Map<String, String> fila) {
        return CAMPOS_OBLIGATORIOS.stream()
                .filter(campo -> fila.getOrDefault(campo, "").isBlank())
                .toList();
    }

    private static String motivo(List<String> faltantes) {
        String lista = String.join(", ", faltantes);
        return faltantes.size() == 1
                ? "falta el campo " + lista
                : "faltan los campos " + lista;
    }

    private static Contrato aContrato(Map<String, String> fila, List<String> encabezado) {
        Map<String, String> adicionales = new LinkedHashMap<>();
        for (String columna : encabezado) {
            if (!COLUMNAS_REQUERIDAS.contains(columna)) {
                adicionales.put(columna, fila.get(columna));
            }
        }
        return new Contrato(
                fila.get("numero_contrato"),
                fila.get("entidad"),
                fila.get("contratista"),
                fila.get("funcionario"),
                fila.get("monto"),
                fila.get("fecha"),
                adicionales);
    }
}
