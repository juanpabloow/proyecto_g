package dac.pruebas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Arnés de pruebas mínimo, sin dependencias externas.
 *
 * <p>El equipo no tiene Maven ni Gradle instalados, y bajar JUnit a mano para
 * doce pruebas cuesta más que escribirlas así. Cuando el proyecto adopte un
 * gestor de dependencias, cada {@code caso(...)} se convierte en un
 * {@code @Test} sin tocar las aserciones.
 */
public final class Pruebas {

    /** Un cuerpo de prueba que puede fallar. */
    public interface Caso {
        void ejecutar() throws Exception;
    }

    public static final String ENCABEZADO =
            "numero_contrato,entidad,contratista,funcionario,monto,fecha\n";

    private static final List<String> FALLOS = new ArrayList<>();
    private static int total = 0;

    private Pruebas() {
    }

    /** Deja claro, antes de la primera prueba, qué entrega este módulo. */
    public static void encabezado() {
        System.out.println("DAC - modulo Java");
        System.out.println("Historia entregada: HU-04. El resto es la base que HU-04 necesita.");
    }

    public static void grupo(String titulo) {
        System.out.println();
        System.out.println(titulo);
    }

    public static void caso(String nombre, Caso caso) {
        total++;
        try {
            caso.ejecutar();
            System.out.println("  [ok]  " + nombre);
        } catch (Throwable fallo) {
            FALLOS.add(nombre + " -> " + fallo.getMessage());
            System.out.println("  [NO]  " + nombre + " -> " + fallo.getMessage());
        }
    }

    public static void cierto(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }

    public static void igual(Object esperado, Object obtenido, String mensaje) {
        if (!Objects.equals(esperado, obtenido)) {
            throw new AssertionError(mensaje + " | esperado <" + esperado + ">, obtenido <" + obtenido + ">");
        }
    }

    /** Verifica que algo falle, del tipo esperado y con un mensaje que dice qué pasó. */
    public static void lanza(Class<? extends Throwable> tipo, String fragmentoEsperado, Caso caso) {
        try {
            caso.ejecutar();
        } catch (Throwable lanzado) {
            if (!tipo.isInstance(lanzado)) {
                throw new AssertionError("se esperaba " + tipo.getSimpleName()
                        + " y llegó " + lanzado.getClass().getSimpleName() + ": " + lanzado.getMessage());
            }
            String mensaje = String.valueOf(lanzado.getMessage());
            if (!mensaje.contains(fragmentoEsperado)) {
                throw new AssertionError("el error no menciona \"" + fragmentoEsperado + "\": " + mensaje);
            }
            return;
        }
        throw new AssertionError("se esperaba " + tipo.getSimpleName() + " y no falló nada");
    }

    /** Escribe un CSV temporal y devuelve su ruta. */
    public static Path csv(String contenido) {
        try {
            Path directorio = Files.createTempDirectory("dac-pruebas");
            directorio.toFile().deleteOnExit();
            Path ruta = directorio.resolve("contratos.csv");
            Files.writeString(ruta, contenido, StandardCharsets.UTF_8);
            ruta.toFile().deleteOnExit();
            return ruta;
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo crear el CSV de prueba", error);
        }
    }

    /** Imprime el resultado y devuelve el código de salida (0 = todo pasó). */
    public static int resumen() {
        System.out.println();
        System.out.println("-".repeat(60));
        if (FALLOS.isEmpty()) {
            System.out.println(total + " pruebas, todas pasando.");
            return 0;
        }
        System.out.println(total + " pruebas, " + FALLOS.size() + " fallando:");
        for (String fallo : FALLOS) {
            System.out.println("  - " + fallo);
        }
        return 1;
    }
}
