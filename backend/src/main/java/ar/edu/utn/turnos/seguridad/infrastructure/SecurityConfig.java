package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.shared.web.RespuestasSeguridadProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.web.access.intercept.AuthorizationFilter;
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
            @Value("${APP_CORS_ORIGIN:}") String origen, ObjectProvider<EstadoSesionPersonal> estadoSesion)
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
                        // Denegar escrituras de bloqueos antes de que MVC valide el cuerpo o los IDs.
                        .requestMatchers(HttpMethod.POST, "/api/profesionales/{id}/bloqueos")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/profesionales/{id}/bloqueos/{bloqueoId}")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        // Ídem para los horarios de atención (C-11).
                        .requestMatchers(HttpMethod.POST, "/api/profesionales/{id}/horarios")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.PUT, "/api/profesionales/{id}/horarios/{horarioId}")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/profesionales/{id}/horarios/{horarioId}")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        // Gestión de usuarios (C-13): solo ADMIN, también para leer; 403 antes de validar el cuerpo.
                        .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")
                        // Lista de espera (C-18): el odontólogo solo lee; 403 antes de validar el cuerpo.
                        .requestMatchers(HttpMethod.POST, "/api/lista-espera").hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/lista-espera/{id}").hasAnyRole("ADMIN", "RECEPCION")
                        // Sugerencias de un hueco (C-31): sirven para asignarlo, así que el odontólogo no las ve.
                        .requestMatchers(HttpMethod.GET, "/api/lista-espera/compatibles")
                            .hasAnyRole("ADMIN", "RECEPCION")
                        // Turnos (C-19): el odontólogo no da turnos (RN-20, RN-33); 403 antes de validar el cuerpo.
                        .requestMatchers(HttpMethod.POST, "/api/turnos").hasAnyRole("ADMIN", "RECEPCION")
                        // Cancelar (C-27): ídem; el odontólogo recibe 403 antes de validar el motivo.
                        .requestMatchers(HttpMethod.POST, "/api/turnos/{id}/cancelar").hasAnyRole("ADMIN", "RECEPCION")
                        // Reprogramar (C-35): ídem; el paciente tampoco reprograma por token (RN-18).
                        .requestMatchers(HttpMethod.POST, "/api/turnos/{id}/reprogramar").hasAnyRole("ADMIN", "RECEPCION")
                        // Asistencia (C-29): el odontólogo no marca ni corrige (RN-20); 403 antes de validar el estado.
                        // La corrección solo por ADMIN depende del estado del turno: la decide el servicio.
                        .requestMatchers(HttpMethod.POST, "/api/turnos/{id}/asistencia").hasAnyRole("ADMIN", "RECEPCION")
                        // Registro de notificaciones (C-25, US-020): el odontólogo no lo consulta.
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones").hasAnyRole("ADMIN", "RECEPCION")
                        // Regla general de RN-33 (C-28): el odontólogo es solo lectura. Va después de las reglas
                        // específicas (login y públicas ya se resolvieron arriba; logout lo atiende LogoutFilter)
                        // y da 403 antes de que MVC valide el cuerpo. Las restricciones solo-ADMIN siguen aplicando.
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyRole("ADMIN", "RECEPCION")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "RECEPCION")
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
        // C-13: una sesión de un usuario desactivado o con otro rol o email pasa a anónima (401). En los
        // slices @WebMvcTest no hay JPA y el verificador no existe, así que el filtro no se agrega.
        estadoSesion.ifAvailable(estado ->
                http.addFilterBefore(new VerificacionSesionVigente(estado), AuthorizationFilter.class));
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
