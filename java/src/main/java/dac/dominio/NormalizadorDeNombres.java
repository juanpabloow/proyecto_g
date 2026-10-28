package dac.dominio;

import java.text.Normalizer;
import java.util.Locale;

/**
 * HU-05 — Reconocer el mismo nombre escrito de formas distintas (R-5).
 *
 * <p>Normalización conservadora, decidida de forma provisional (Decisión 19):
 * solo quita lo que no cambia la identidad de una persona o empresa.
 * <ul>
 *   <li>mayúsculas y minúsculas: "ACME SAS" = "acme sas"</li>
 *   <li>espacios sobrantes al inicio, al final o repetidos</li>
 *   <li>tildes: "Juan Pérez" = "Juan Perez"</li>
 *   <li>puntos y comas: "ACME S.A.S." = "ACME SAS"</li>
 * </ul>
 * La ñ se conserva: "Peña" y "Pena" son apellidos distintos. No se quitan
 * sufijos societarios ni se usan alias: "ACME SAS" y "ACME LTDA" siguen
 * siendo empresas distintas, porque unirlas podría señalar a quien no es (S-6).
 *
 * <p>La normalización solo sirve para <em>comparar</em>. Lo que se guarda y
 * se muestra es el nombre original del archivo (criterio 2 de HU-05).
 */
public final class NormalizadorDeNombres {

    private static final String MARCA_ENE = "\u0000";

    private NormalizadorDeNombres() {}

    public static String normalizar(String nombre) {
        if (nombre == null) return "";
        String texto = nombre.toLowerCase(Locale.ROOT).replace("ñ", MARCA_ENE);
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        texto = texto.replace(MARCA_ENE, "ñ");
        texto = texto.replaceAll("[.,]", "");
        return texto.strip().replaceAll("\\s+", " ");
    }
}
