package dac.dominio;

import java.util.ArrayList;
import java.util.List;

/** Lector de CSV mínimo sin dependencias externas. Maneja comillas y escapes. */
final class LectorCSV {

    private LectorCSV() {}

    static List<List<String>> filas(String texto) {
        List<List<String>> filas = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean filaEmpezada = false;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"'); i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    campo.append(c);
                }
                continue;
            }
            switch (c) {
                case '"' -> { entreComillas = true; filaEmpezada = true; }
                case ',' -> { campos.add(campo.toString()); campo.setLength(0); filaEmpezada = true; }
                case '\n' -> {
                    campos.add(campo.toString()); campo.setLength(0);
                    filas.add(campos); campos = new ArrayList<>(); filaEmpezada = false;
                }
                case '\r' -> { /* Windows line ending, el \n cierra la fila */ }
                default  -> { campo.append(c); filaEmpezada = true; }
            }
        }
        if (filaEmpezada || !campo.isEmpty()) {
            campos.add(campo.toString());
            filas.add(campos);
        }
        return filas;
    }

    static boolean estaEnBlanco(List<String> fila) {
        return fila.stream().allMatch(String::isBlank);
    }
}
