package ar.edu.utn.turnos.shared.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.seguridad.infrastructure.AutenticacionPersonal;
import ar.edu.utn.turnos.seguridad.infrastructure.SecurityConfig;
import ar.edu.utn.turnos.shared.web.ManejadorErrores;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = FiltroLimiteTasaPublicoTest.ControladorPrueba.class,
        properties = {
            "app.ratelimit.max-solicitudes-por-ip=2",
            "app.ratelimit.ventana-ip=45s",
            "app.ratelimit.max-solicitudes-por-clave=2",
            "app.ratelimit.ventana-clave=30s"
        })
@Import({
    SecurityConfig.class,
    ManejadorErrores.class,
    RateLimitConfig.class,
    FiltroLimiteTasaPublicoTest.RelojMutableTestConfig.class,
    FiltroLimiteTasaPublicoTest.ControladorPrueba.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FiltroLimiteTasaPublicoTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");

    @MockitoBean
    private AutenticacionPersonal autenticacionPersonal;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LimitadorTasaEnMemoriaTest.RelojMutable reloj;

    @Autowired
    private ControladorPrueba controlador;

    @TestConfiguration(proxyBeanMethods = false)
    static class RelojMutableTestConfig {

        @Bean
        @Primary
        LimitadorTasaEnMemoriaTest.RelojMutable relojMutable() {
            return new LimitadorTasaEnMemoriaTest.RelojMutable(T0);
        }

        @Bean
        Clock clock(LimitadorTasaEnMemoriaTest.RelojMutable relojMutable) {
            return relojMutable;
        }
    }

    @RestController
    static class ControladorPrueba {

        private final AtomicInteger invocacionesPublicas = new AtomicInteger();

        @GetMapping("/api/public/disponibilidad-prueba")
        public String publica() {
            invocacionesPublicas.incrementAndGet();
            return "ok-publico";
        }

        @GetMapping("/api/turnos/privada-prueba")
        public String privada() {
            return "ok-privado";
        }

        int invocacionesPublicas() {
            return invocacionesPublicas.get();
        }
    }

    @Test
    @DisplayName("Las solicitudes públicas dentro del umbral se permiten")
    void lasSolicitudesPublicasDentroDelUmbralSePermiten() throws Exception {
        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.10");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-publico"));

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.10");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-publico"));
    }

    @Test
    @DisplayName("Superar el umbral por IP en una ruta pública devuelve 429 con Retry-After")
    void superarElUmbralPorIpEnUnaRutaPublicaDevuelve429ConRetryAfter() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                        req.setRemoteAddr("198.51.100.20");
                        return req;
                    }))
                    .andExpect(status().isOk());
        }

        reloj.avanzar(Duration.ofSeconds(15));

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.20");
                    return req;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.codigo").value("RN-27"));

        org.assertj.core.api.Assertions.assertThat(controlador.invocacionesPublicas()).isEqualTo(2);

        reloj.avanzar(Duration.ofSeconds(30));

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.20");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-publico"));
    }

    @Test
    @DisplayName("El bloqueo de una IP no afecta a otra IP distinta")
    void elBloqueoDeUnaIpNoAfectaAOtraIpDistinta() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                        req.setRemoteAddr("198.51.100.30");
                        return req;
                    }))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.30");
                    return req;
                }))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.31");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-publico"));
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Las rutas fuera de la zona pública no son limitadas por el filtro público")
    void lasRutasFueraDeLaZonaPublicaNoSonLimitadasPorElFiltroPublico() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                        req.setRemoteAddr("198.51.100.40");
                        return req;
                    }))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/public/disponibilidad-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.40");
                    return req;
                }))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/turnos/privada-prueba").with(req -> {
                    req.setRemoteAddr("198.51.100.40");
                    return req;
                }))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-privado"));
    }
}
