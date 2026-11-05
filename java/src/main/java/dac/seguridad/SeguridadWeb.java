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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Inicio de sesión del analista (S-10, Módulo 7). Antes DAC no tenía sesión:
 * cualquiera con acceso al puerto 8080 usaba el sistema (deuda DT-03). Ahora
 * hay un único usuario, tomado de la configuración ({@code dac.auth.*}), con
 * la clave guardada como <b>hash BCrypt</b> en memoria, nunca en texto plano.
 *
 * <ul>
 *   <li><b>Formulario</b> ({@code /login.html}) para el analista en el navegador
 *       y <b>HTTP Basic</b> para clientes programáticos y las pruebas.</li>
 *   <li><b>Cabeceras:</b> las escribe {@code CabecerasDeSeguridad}; aquí se
 *       desactivan las de Spring Security para no duplicarlas ni pisar el
 *       {@code Cache-Control: no-store} que define la API (S-01).</li>
 *   <li><b>CSRF:</b> desactivado en este alcance (deuda DT-16). El único
 *       endpoint que cambia estado es la carga del CSV, usada por un solo
 *       analista en localhost; la protección CSRF se añade junto con HTTPS
 *       (DT-14) al desplegar.</li>
 *   <li>{@code /actuator/**}, la página de login y los estilos quedan abiertos;
 *       todo lo demás exige sesión.</li>
 * </ul>
 */
@Configuration
public class SeguridadWeb {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService usuarios(PasswordEncoder encoder,
                                       @Value("${dac.auth.usuario}") String usuario,
                                       @Value("${dac.auth.clave}") String clave) {
        UserDetails analista = User.withUsername(usuario)
                .password(encoder.encode(clave)) // se guarda el hash, no la clave
                .roles("ANALISTA")
                .build();
        return new InMemoryUserDetailsManager(analista);
    }

    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/login", "/css/**", "/js/login.js", "/actuator/**", "/error").permitAll()
                .anyRequest().authenticated())
            // La API siempre responde 401 (nunca redirige al formulario): así un
            // cliente que reenvía la cabecera no entra en un bucle de redirecciones.
            // Las páginas del navegador sí van al login (formLogin, más abajo).
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/**")))
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
            .headers(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
