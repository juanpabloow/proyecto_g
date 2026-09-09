package dac;

import java.nio.file.Path;
import java.util.List;

/**
 * El flujo completo de punta a punta: cargar, detectar, mostrar.
 *
 * <p>Se ejecuta desde la raíz del repositorio. Sin argumentos usa el CSV de
 * ejemplo; con un argumento, el archivo que se le pase.
 */
public final class Main {

    private static final Path RUTA_EJEMPLO = Path.of("data", "contratos_ejemplo.csv");

    private Main() {
    }

    public static void main(String[] args) {
        Path ruta = args.length > 0 ? Path.of(args[0]) : RUTA_EJEMPLO;

        ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);
        System.out.println(carga.resumen());

        List<Alerta> alertas = new DetectorDeReincidencias().detectar(carga.contratos());
        System.out.println("Alertas encontradas: " + alertas.size());
        System.out.println();

        for (Alerta alerta : alertas) {
            System.out.println(alerta.mostrar());
        }
    }
}
