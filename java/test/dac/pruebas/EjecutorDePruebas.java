package dac.pruebas;

/** Corre todas las pruebas del módulo. Sale con código 1 si alguna falla. */
public final class EjecutorDePruebas {

    private EjecutorDePruebas() {
    }

    public static void main(String[] args) {
        Pruebas.encabezado();

        // Primero la historia que entrega este módulo.
        PruebasFilasIncompletas.ejecutar();

        // Después la base que HU-04 necesita para poder existir: sin cargador
        // no hay filas que descartar, y sin detector no se puede comprobar
        // que la fila descartada "no genera ninguna alerta".
        PruebasCarga.ejecutar();
        PruebasReincidencia.ejecutar();

        System.exit(Pruebas.resumen());
    }
}
