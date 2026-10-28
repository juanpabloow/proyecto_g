package dac.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeceras de seguridad básicas en todas las respuestas (Módulo 7, S-01).
 *
 * <ul>
 *   <li>{@code Content-Security-Policy}: la página solo carga scripts, estilos e
 *       imágenes del propio servidor. Un {@code <script>} que llegara dentro de
 *       un dato no se ejecuta, aunque la interfaz fallara al escaparlo.</li>
 *   <li>{@code X-Content-Type-Options: nosniff}: el navegador no "adivina" el
 *       tipo; un JSON o un CSV nunca se interpreta como HTML.</li>
 *   <li>{@code X-Frame-Options: DENY}: nadie puede meter la página en un iframe
 *       para engañar al analista (clickjacking).</li>
 *   <li>{@code Referrer-Policy: no-referrer}: no se filtran direcciones internas.</li>
 *   <li>{@code Cache-Control: no-store} en la API: las alertas (datos
 *       sensibles, S-8) no quedan guardadas en cachés intermedias.</li>
 * </ul>
 */
@Component
public class CabecerasDeSeguridad extends OncePerRequestFilter {

    static final String POLITICA_DE_CONTENIDO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'none'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        respuesta.setHeader("Content-Security-Policy", POLITICA_DE_CONTENIDO);
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        respuesta.setHeader("X-Frame-Options", "DENY");
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        if (peticion.getRequestURI().startsWith("/api/")) {
            respuesta.setHeader("Cache-Control", "no-store");
        }
        cadena.doFilter(peticion, respuesta);
    }
}
