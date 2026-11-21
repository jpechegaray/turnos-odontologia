package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.shared.web.RespuestasSeguridadProblemDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Política base de acceso: todo denegado salvo la zona pública. C-03 la reemplaza agregando el login del
 * personal (cookie de sesión, CSRF con cookie, CORS) y reutiliza {@link RespuestasSeguridadProblemDetail}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(RespuestasSeguridadProblemDetail.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
        "/api/public/**", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http, RespuestasSeguridadProblemDetail respuestas)
            throws Exception {
        http
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/public/**"))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler(respuestas));
        return http.build();
    }

    /** Sin usuarios: evita que Boot genere el usuario "user" con contraseña aleatoria en el log. */
    @Bean
    UserDetailsService usuarios() {
        return new InMemoryUserDetailsManager();
    }
}
