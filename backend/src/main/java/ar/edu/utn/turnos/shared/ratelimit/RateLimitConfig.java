package ar.edu.utn.turnos.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuración del límite de tasa para endpoints públicos ({@code /api/public/**}) y ayudante
 * reutilizable por clave. Registra el filtro con {@link FilterRegistrationBean} sin editar
 * {@code SecurityConfig}.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public LimitadorPorClave limitadorPorClave(RateLimitProperties propiedades, Clock reloj) {
        return new LimitadorPorClave(propiedades, reloj);
    }

    @Bean
    public FiltroLimiteTasaPublico filtroLimiteTasaPublico(
            RateLimitProperties propiedades, Clock reloj, ObjectMapper objectMapper) {
        var limitadorIp = new LimitadorTasaEnMemoria(
                propiedades.maxSolicitudesPorIp(),
                propiedades.ventanaIp(),
                reloj);
        return new FiltroLimiteTasaPublico(limitadorIp, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<FiltroLimiteTasaPublico> registroFiltroLimiteTasaPublico(
            FiltroLimiteTasaPublico filtro) {
        var registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns("/api/public/*");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registro;
    }
}
