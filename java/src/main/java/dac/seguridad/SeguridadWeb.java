package dac.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Inicio de sesión del analista (S-10, Módulo 7). Antes DAC no tenía sesión:
 * cualquiera con acceso al puerto 8080 usaba el sistema (deuda DT-03). Ahora
 * hay un único usuario, tomado de la configuración ({@code dac.auth.*}), con
 * la clave guardada como <b>hash BCrypt</b> en memoria, nunca en texto plano.
 * Fuera del perfil {@code dev} no hay credencial por defecto: si falta, la
 * aplicación no arranca ({@link #exigirCredencial}).
 *
 * <ul>
 *   <li><b>Formulario</b> ({@code /login.html}) para el analista en el navegador
 *       y <b>HTTP Basic</b> para clientes programáticos y las pruebas.</li>
 *   <li><b>Cabeceras (S-01):</b> las escribe Spring Security en <b>todas</b> las
 *       respuestas, incluidas las que él mismo rechaza (401): CSP, {@code nosniff},
 *       {@code X-Frame-Options: DENY}, {@code Referrer-Policy: no-referrer} y
 *       {@code Cache-Control: no-store}. Antes las escribía un filtro propio que
 *       corría después de Spring Security, así que un 401 salía sin ellas.</li>
 *   <li><b>CSRF:</b> desactivado en este alcance (deuda DT-16). El único
 *       endpoint que cambia estado es la carga del CSV, usada por un solo
 *       analista en localhost; la protección CSRF se añade junto con HTTPS
 *       (DT-14) al desplegar.</li>
 *   <li>Quedan abiertos la página de login, sus estilos y script, y
 *       {@code /actuator/health} (para saber si el servicio está vivo). Todo lo
 *       demás exige sesión, incluidos {@code /actuator/info} y
 *       {@code /actuator/prometheus}.</li>
 * </ul>
 */
@Configuration
public class SeguridadWeb {

    /** La página solo carga scripts, estilos e imágenes del propio servidor. */
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService usuarios(PasswordEncoder encoder,
                                       @Value("${dac.auth.usuario}") String usuario,
                                       @Value("${dac.auth.clave}") String clave) {
        exigirCredencial(usuario, clave);
        UserDetails analista = User.withUsername(usuario)
                .password(encoder.encode(clave)) // se guarda el hash, no la clave
                .roles("ANALISTA")
                .build();
        return new InMemoryUserDetailsManager(analista);
    }

    /**
     * Sin usuario o sin clave configurados la aplicación no arranca: es mejor
     * fallar con un mensaje claro que quedar con una credencial conocida.
     */
    static void exigirCredencial(String usuario, String clave) {
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta la credencial del analista: define DAC_AUTH_USER y "
                    + "DAC_AUTH_PASSWORD en el archivo .env (o como variables de entorno), "
                    + "o arranca con el perfil de desarrollo: java/ejecutar.sh dev");
        }
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/login", "/css/**", "/js/login.js",
                                 "/actuator/health", "/actuator/health/**", "/error").permitAll()
                .anyRequest().authenticated())
            // La API y Actuator siempre responden 401 (nunca redirigen al
            // formulario): así un cliente que reenvía la cabecera no entra en un
            // bucle de redirecciones. Las páginas del navegador sí van al login.
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                new OrRequestMatcher(new AntPathRequestMatcher("/api/**"),
                                     new AntPathRequestMatcher("/actuator/**"))))
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?error")
                .permitAll())
            // HTTP Basic: una credencial inválida devuelve 401 directo (setStatus,
            // no sendError: sendError re-despacha a /error y, en el navegador, eso
            // terminaba en una redirección al login y un bucle). Sin cabecera
            // WWW-Authenticate para no disparar el diálogo nativo del navegador
            // (el navegador usa el formulario de login, no Basic).
            .httpBasic(basic -> basic.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .logout(salir -> salir
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?salida")
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            // nosniff y Cache-Control (no-cache, no-store, max-age=0,
            // must-revalidate) vienen activos por defecto en Spring Security.
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_DE_CONTENIDO))
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }
}
