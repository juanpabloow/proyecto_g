package dac.pruebas;

import static dac.pruebas.Pruebas.ENCABEZADO;
import static dac.pruebas.Pruebas.caso;
import static dac.pruebas.Pruebas.cierto;
import static dac.pruebas.Pruebas.csv;
import static dac.pruebas.Pruebas.grupo;
import static dac.pruebas.Pruebas.igual;

import dac.CargadorDeContratos;
import dac.DetectorDeReincidencias;
import dac.FilaDescartada;
import dac.ResultadoCarga;
import java.nio.file.Path;

/**
 * HU-04 - No generar alertas a partir de filas incompletas.
 *
 * <p>Implementa la Decisión 12: la fila incompleta se descarta y se reporta;
 * el archivo sigue siendo válido. Antes de esta historia, dos filas vacías
 * producían una alerta del par ("", ""): una alerta sobre nadie (R-6).
 */
final class PruebasFilasIncompletas {

    private PruebasFilasIncompletas() {
    }

    static void ejecutar() {
        grupo("HU-04 - No generar alertas a partir de filas incompletas  <-- la historia");

        caso("criterio 1: dos filas sin contratista ni funcionario no generan alerta", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,,,100,2026-01-01\n"
                    + "002,Alcaldía,,,200,2026-01-02\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(0, carga.contratos().size(), "ninguna de las dos filas es utilizable");
            cierto(new DetectorDeReincidencias().detectar(carga.contratos()).isEmpty(),
                    "ya no se genera la alerta del par vacío (R-6)");
        });

        caso("criterio 2: informa cuántas filas se descartaron y por qué", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "002,Alcaldía,,Ana Ruiz,200,2026-01-02\n"
                    + "003,Alcaldía,,,300,2026-01-03\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(2, carga.cantidadDescartadas(), "dos filas incompletas");

            FilaDescartada primera = carga.filasDescartadas().get(0);
            igual(3, primera.fila(), "la fila 1 es el encabezado, así que esta es la 3");
            cierto(primera.motivo().contains("contratista"),
                    "el motivo debe nombrar el campo que falta: " + primera.motivo());

            FilaDescartada segunda = carga.filasDescartadas().get(1);
            cierto(segunda.motivo().contains("contratista") && segunda.motivo().contains("funcionario"),
                    "cuando faltan dos campos, el motivo nombra los dos: " + segunda.motivo());

            cierto(carga.resumen().contains("Filas descartadas: 2"),
                    "el resumen que ve el analista debe decir cuántas se descartaron");
        });

        caso("una fila incompleta no invalida el archivo (Decisión 12)", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "002,Alcaldía,,,200,2026-01-02\n"
                    + "003,Gobernación,ACME SAS,Juan Pérez,300,2026-01-03\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(2, carga.contratos().size(), "las filas buenas se cargan igual");
            igual(1, new DetectorDeReincidencias().detectar(carga.contratos()).size(),
                    "y la reincidencia real se sigue detectando");
        });

        caso("una celda con solo espacios cuenta como vacía", () -> {
            Path ruta = csv(ENCABEZADO + "001,Alcaldía,   ,Juan Pérez,100,2026-01-01\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(0, carga.contratos().size(), "un espacio no es un contratista");
            igual(1, carga.cantidadDescartadas(), "y se reporta como descartada");
        });

        caso("una fila sin clave natural se descarta: no se puede deduplicar (R-2)", () -> {
            Path ruta = csv(ENCABEZADO + ",Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(0, carga.contratos().size(), "sin número de contrato no hay identidad");
            cierto(carga.filasDescartadas().get(0).motivo().contains("numero_contrato"),
                    "el motivo nombra el campo faltante");
        });

        caso("monto y fecha vacíos NO descartan la fila (R-8)", () -> {
            Path ruta = csv(ENCABEZADO + "001,Alcaldía,ACME SAS,Juan Pérez,,\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(1, carga.contratos().size(),
                    "la señal no usa monto ni fecha: descartar la fila perdería una reincidencia real");
            igual(0, carga.cantidadDescartadas(), "no hay nada que reportar");
        });

        caso("una línea en blanco no se reporta como fila descartada", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(1, carga.contratos().size(), "el contrato bueno se carga");
            igual(0, carga.cantidadDescartadas(), "una línea vacía no es un problema de calidad de datos");
        });
    }
}
