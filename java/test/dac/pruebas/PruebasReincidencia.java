package dac.pruebas;

import static dac.pruebas.Pruebas.caso;
import static dac.pruebas.Pruebas.cierto;
import static dac.pruebas.Pruebas.grupo;
import static dac.pruebas.Pruebas.igual;
import static dac.pruebas.Pruebas.lanza;

import dac.Alerta;
import dac.Contrato;
import dac.DetectorDeReincidencias;
import java.util.List;

/**
 * HU-02 - Detectar reincidencia contratista-funcionario (historia central).
 *
 * <p>Un caso por cada criterio de aceptación de la historia.
 * Equivale a {@code tests/test_alertas.py} de la versión en Python.
 */
final class PruebasReincidencia {

    private PruebasReincidencia() {
    }

    private static Contrato contrato(String numero, String contratista, String funcionario) {
        return Contrato.de(numero, "Alcaldía", contratista, funcionario, "100", "2026-01-01");
    }

    static void ejecutar() {
        grupo("Base | HU-02 - Detectar reincidencia contratista-funcionario");

        caso("criterio 1: un par repetido genera una alerta", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "ACME SAS", "Juan Pérez"));

            List<Alerta> alertas = new DetectorDeReincidencias().detectar(contratos);

            igual(1, alertas.size(), "el par se repite: debe haber una alerta");
            igual("ACME SAS", alertas.get(0).contratista(), "contratista señalado");
            igual("Juan Pérez", alertas.get(0).funcionario(), "funcionario señalado");
        });

        caso("criterio 2: sin repeticiones no hay alertas", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "Otra Ltda", "Ana Ruiz"));

            cierto(new DetectorDeReincidencias().detectar(contratos).isEmpty(),
                    "ningún par se repite");
        });

        caso("criterio 3: un par en 3 contratos da una sola alerta con 3 evidencias", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "ACME SAS", "Juan Pérez"),
                    contrato("003", "ACME SAS", "Juan Pérez"));

            List<Alerta> alertas = new DetectorDeReincidencias().detectar(contratos);

            igual(1, alertas.size(), "una alerta por par, no una por contrato");
            igual(3, alertas.get(0).cantidadContratos(), "los tres contratos son la evidencia");
        });

        caso("criterio 4: mismo contratista con funcionarios distintos no alerta", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "ACME SAS", "Ana Ruiz"));

            cierto(new DetectorDeReincidencias().detectar(contratos).isEmpty(),
                    "la señal es el par, no el contratista por sí solo");
        });

        caso("criterio 5: toda alerta trae al menos 2 contratos de evidencia", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "ACME SAS", "Juan Pérez"),
                    contrato("003", "Otra Ltda", "Ana Ruiz"),
                    contrato("004", "Constructora XYZ", "Carlos Gómez"),
                    contrato("005", "Constructora XYZ", "Carlos Gómez"));

            List<Alerta> alertas = new DetectorDeReincidencias().detectar(contratos);

            igual(2, alertas.size(), "dos pares reincidentes");
            for (Alerta alerta : alertas) {
                cierto(alerta.cantidadContratos() >= 2,
                        "una alerta sin evidencia es un defecto: " + alerta.contratista());
            }
        });

        caso("criterio 5: construir una alerta con un solo contrato es un error", () ->
                lanza(IllegalArgumentException.class, "defecto", () ->
                        new Alerta("ACME SAS", "Juan Pérez",
                                List.of(contrato("001", "ACME SAS", "Juan Pérez")))));

        caso("criterio 7: la alerta muestra número, entidad, fecha y monto", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "ACME SAS", "Juan Pérez"));

            String salida = new DetectorDeReincidencias().detectar(contratos).get(0).mostrar();

            cierto(salida.contains("001"), "falta el número de contrato");
            cierto(salida.contains("Alcaldía"), "falta la entidad");
            cierto(salida.contains("2026-01-01"), "falta la fecha");
            cierto(salida.contains("100"), "falta el monto");
        });

        caso("limitación conocida: nombres escritos distinto no se unen (R-5, HU-05)", () -> {
            List<Contrato> contratos = List.of(
                    contrato("001", "ACME SAS", "Juan Pérez"),
                    contrato("002", "acme sas", "juan pérez"));

            cierto(new DetectorDeReincidencias().detectar(contratos).isEmpty(),
                    "hoy son contratistas distintos: falso negativo documentado, no un descuido");
        });
    }
}
