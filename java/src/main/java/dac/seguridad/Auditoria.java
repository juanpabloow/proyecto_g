package dac.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registro de eventos críticos (Módulo 7, S-07): cada carga aceptada o
 * rechazada y cada exportación del reporte quedan en el log con fecha y hora.
 *
 * <p>Se registran <b>cantidades, no nombres</b>: el log no debe convertirse en
 * otra copia de los datos sensibles (S-8). Los textos que vienen del usuario
 * (nombre del archivo, mensaje de error) se limpian de saltos de línea para
 * que nadie pueda inventar líneas falsas en el registro (inyección en logs).
 */
public final class Auditoria {

    private static final Logger LOG = LoggerFactory.getLogger("dac.auditoria");

    private Auditoria() {}

    public static void cargaAceptada(String archivo, long bytes, int nuevos, int totalEnBd,
                                     int descartadas, int conflictos, int reincidencias,
                                     int fraccionamientos) {
        LOG.info("AUDITORIA carga aceptada archivo=\"{}\" bytes={} nuevos={} totalEnBd={} "
                        + "descartadas={} conflictos={} reincidencias={} fraccionamientos={}",
                limpio(archivo), bytes, nuevos, totalEnBd, descartadas, conflictos,
                reincidencias, fraccionamientos);
    }

    public static void cargaRechazada(String archivo, String motivo) {
        LOG.warn("AUDITORIA carga rechazada archivo=\"{}\" motivo=\"{}\"", limpio(archivo), limpio(motivo));
    }

    public static void exportacion(int filas) {
        LOG.info("AUDITORIA reporte exportado filas={}", filas);
    }

    public static void errorInesperado(String donde, Exception e) {
        LOG.error("AUDITORIA error inesperado en {}", donde, e);
    }

    /** Quita saltos de línea y caracteres de control, y recorta a 200 caracteres. */
    static String limpio(String texto) {
        if (texto == null) return "";
        String sinControl = texto.replaceAll("[\\p{Cntrl}]", " ");
        return sinControl.length() > 200 ? sinControl.substring(0, 200) + "…" : sinControl;
    }
}
