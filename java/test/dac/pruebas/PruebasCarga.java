package dac.pruebas;

import static dac.pruebas.Pruebas.ENCABEZADO;
import static dac.pruebas.Pruebas.caso;
import static dac.pruebas.Pruebas.cierto;
import static dac.pruebas.Pruebas.csv;
import static dac.pruebas.Pruebas.grupo;
import static dac.pruebas.Pruebas.igual;
import static dac.pruebas.Pruebas.lanza;

import dac.CargadorDeContratos;
import dac.Contrato;
import dac.DetectorDeReincidencias;
import dac.ResultadoCarga;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HU-01 (cargar contratos desde CSV) y el problema duro (idempotencia).
 *
 * <p>Equivale a {@code tests/test_contratos.py} de la versión en Python.
 */
final class PruebasCarga {

    private static final String FILA = "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n";

    private PruebasCarga() {
    }

    static void ejecutar() {
        grupo("Base | HU-01 - Cargar contratos desde un archivo CSV");

        caso("carga un contrato por cada fila válida", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "002,Alcaldía,Otra Ltda,Ana Ruiz,200,2026-01-02\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(2, carga.contratos().size(), "deberían cargarse las dos filas");
            igual("ACME SAS", carga.contratos().get(0).contratista(), "primer contratista");
        });

        caso("falla nombrando la columna que falta, no con un error genérico", () -> {
            Path ruta = csv("numero_contrato,entidad,contratista\n001,Alcaldía,ACME SAS\n");

            lanza(IllegalArgumentException.class, "funcionario",
                    () -> new CargadorDeContratos().cargarDesdeCSV(ruta));
        });

        caso("falla con mensaje claro si el archivo está vacío", () -> {
            Path ruta = csv("");

            lanza(IllegalArgumentException.class, "vacío",
                    () -> new CargadorDeContratos().cargarDesdeCSV(ruta));
        });

        caso("conserva las columnas adicionales del CSV (R-1)", () -> {
            Path ruta = csv(ENCABEZADO.strip() + ",objeto\n"
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01,Suministro de papelería\n");

            Contrato contrato = new CargadorDeContratos().cargarDesdeCSV(ruta).contratos().get(0);

            igual("Suministro de papelería", contrato.camposAdicionales().get("objeto"),
                    "la columna extra debe conservarse");
        });

        caso("lee campos entre comillas que contienen comas", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,\"ACME S.A.S., Sucursal Bogotá\",Juan Pérez,100,2026-01-01\n");

            Contrato contrato = new CargadorDeContratos().cargarDesdeCSV(ruta).contratos().get(0);

            igual("ACME S.A.S., Sucursal Bogotá", contrato.contratista(),
                    "la coma dentro de comillas no parte el campo");
            igual("Juan Pérez", contrato.funcionario(), "las columnas siguientes no se corren");
        });

        grupo("Base | Problema duro - Idempotencia");

        caso("la misma clave natural repetida se carga una sola vez", () -> {
            Path ruta = csv(ENCABEZADO + FILA + FILA + FILA);

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(1, carga.contratos().size(), "tres filas iguales son un solo contrato");
            igual(2, carga.duplicadosIgnorados(), "los dos duplicados quedan reportados");
        });

        caso("un contrato duplicado no genera una alerta falsa", () -> {
            Path ruta = csv(ENCABEZADO + FILA + FILA);

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            cierto(new DetectorDeReincidencias().detectar(carga.contratos()).isEmpty(),
                    "contar dos veces un contrato no es una reincidencia");
        });

        caso("ante datos distintos con la misma clave, gana el primero (Decisión 6)", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "001,Alcaldía,ACME SAS,Juan Pérez,999,2026-01-01\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(1, carga.contratos().size(), "sigue siendo un solo contrato");
            igual("100", carga.contratos().get(0).monto(), "se conserva el monto de la primera fila");
        });

        caso("el mismo número en entidades distintas son dos contratos (R-2)", () -> {
            Path ruta = csv(ENCABEZADO
                    + "001,Alcaldía,ACME SAS,Juan Pérez,100,2026-01-01\n"
                    + "001,Gobernación,ACME SAS,Juan Pérez,200,2026-02-01\n");

            ResultadoCarga carga = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(2, carga.contratos().size(), "el consecutivo 001 se repite entre entidades");
            igual(1, new DetectorDeReincidencias().detectar(carga.contratos()).size(),
                    "y sí son una reincidencia real: mismo par, dos contratos distintos");
        });

        caso("el mismo cargador reconoce un contrato ya cargado desde otro archivo", () -> {
            Path primero = csv(ENCABEZADO + FILA);
            Path segundo = csv(ENCABEZADO + FILA
                    + "002,Alcaldía,Otra Ltda,Ana Ruiz,200,2026-01-02\n");

            CargadorDeContratos cargador = new CargadorDeContratos();
            cargador.cargarDesdeCSV(primero);
            ResultadoCarga carga = cargador.cargarDesdeCSV(segundo);

            igual(1, carga.contratos().size(), "el contrato repetido no se vuelve a cargar");
            igual(1, carga.duplicadosIgnorados(), "y queda contado como duplicado");
        });

        caso("la idempotencia todavía no sobrevive al cierre del programa (HU-07)", () -> {
            Path ruta = csv(ENCABEZADO + FILA);

            // Dos cargadores distintos = dos ejecuciones distintas del programa.
            new CargadorDeContratos().cargarDesdeCSV(ruta);
            ResultadoCarga segunda = new CargadorDeContratos().cargarDesdeCSV(ruta);

            igual(1, segunda.contratos().size(),
                    "limitación conocida y documentada: sin persistencia se vuelve a cargar");
            cierto(Files.exists(ruta), "el archivo sigue ahí");
        });
    }
}
