package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.shared.web.RespuestasSeguridadProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Control de acceso de la API con sesión de servidor y denegación por defecto.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(RespuestasSeguridadProblemDetail.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
        "/api/public/**", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };
    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";

    @Bean
    CookieCsrfTokenRepository tokensCsrf() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityContextRepository contextosDeSeguridad() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy estrategiaSesion(CookieCsrfTokenRepository tokensCsrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(tokensCsrf)));
    }

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http, RespuestasSeguridadProblemDetail respuestas,
            CookieCsrfTokenRepository csrfCookie, SecurityContextRepository contextos,
            @Value("${APP_CORS_ORIGIN:}") String origen)
            throws Exception {
        var login = new AntPathRequestMatcher(LOGIN, "POST");
        var logout = new AntPathRequestMatcher(LOGOUT, "POST");
        var csrfCabecera = new CsrfTokenRequestHandler() {
            private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
                xor.handle(request, response, token);
            }

            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
                return request.getHeader(token.getHeaderName());
            }
        };
        http
                .securityContext(contexto -> contexto.securityContextRepository(contextos))
                .requestCache(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(login).permitAll()
                        .anyRequest().authenticated())
                .cors(cors -> cors.configurationSource(configuracionCors(origen)))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/public/**")
                        .csrfTokenRepository(csrfCookie)
                        .csrfTokenRequestHandler(csrfCabecera))
                .logout(config -> config
                        .logoutRequestMatcher(request -> logout.matches(request) && haySesionAutenticada())
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(
                                org.springframework.http.HttpStatus.NO_CONTENT)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestas)
                        .accessDeniedHandler((request, response, error) -> {
                            if (error instanceof CsrfException && !login.matches(request) && !haySesionAutenticada()) {
                                respuestas.commence(request, response,
                                        new InsufficientAuthenticationException("Se requiere autenticación"));
                            } else {
                                respuestas.handle(request, response, error);
                            }
                        }));
        return http.build();
    }

    private boolean haySesionAutenticada() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken);
    }

    private UrlBasedCorsConfigurationSource configuracionCors(String origen) {
        var fuente = new UrlBasedCorsConfigurationSource();
        if (!origen.isBlank()) {
            var cors = new CorsConfiguration();
            cors.setAllowedOrigins(List.of(origen));
            cors.setAllowCredentials(true);
            cors.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            cors.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
            fuente.registerCorsConfiguration("/api/**", cors);
        }
        return fuente;
    }

    /** Sin usuarios: evita que Boot genere el usuario "user" con contraseña aleatoria en el log. */
    @Bean
    UserDetailsService usuarios() {
        return new InMemoryUserDetailsManager();
    }
}
