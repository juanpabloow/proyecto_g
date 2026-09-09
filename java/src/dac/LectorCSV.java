package dac;

import java.util.ArrayList;
import java.util.List;

/**
 * Lector de CSV mínimo, sin dependencias externas.
 *
 * <p>Entiende lo que necesita un export real: campos entre comillas dobles
 * que contienen comas o saltos de línea, y comillas escapadas duplicándolas
 * ({@code ""}). Un {@code split(",")} partiría por la mitad a un contratista
 * llamado {@code "ACME S.A.S., Sucursal Bogotá"} e inventaría una columna.
 *
 * <p>No interpreta los datos: eso es trabajo de {@link CargadorDeContratos}.
 */
final class LectorCSV {

    private LectorCSV() {
    }

    /** Parte el texto completo en filas de campos, respetando las comillas. */
    static List<List<String>> filas(String texto) {
        List<List<String>> filas = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean filaEmpezada = false;

        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);

            if (entreComillas) {
                boolean comillaEscapada = caracter == '"'
                        && i + 1 < texto.length() && texto.charAt(i + 1) == '"';
                if (comillaEscapada) {
                    campo.append('"');
                    i++;
                } else if (caracter == '"') {
                    entreComillas = false;
                } else {
                    campo.append(caracter);
                }
                continue;
            }

            switch (caracter) {
                case '"' -> {
                    entreComillas = true;
                    filaEmpezada = true;
                }
                case ',' -> {
                    campos.add(campo.toString());
                    campo.setLength(0);
                    filaEmpezada = true;
                }
                case '\n' -> {
                    campos.add(campo.toString());
                    campo.setLength(0);
                    filas.add(campos);
                    campos = new ArrayList<>();
                    filaEmpezada = false;
                }
                case '\r' -> {
                    // Fin de línea de Windows: el '\n' que sigue cierra la fila.
                }
                default -> {
                    campo.append(caracter);
                    filaEmpezada = true;
                }
            }
        }

        // Última fila si el archivo no termina en salto de línea.
        if (filaEmpezada || !campo.isEmpty()) {
            campos.add(campo.toString());
            filas.add(campos);
        }
        return filas;
    }

    /** Una línea en blanco no es una fila de datos: no hay nada que descartar ni que cargar. */
    static boolean estaEnBlanco(List<String> fila) {
        return fila.stream().allMatch(String::isBlank);
    }
}
