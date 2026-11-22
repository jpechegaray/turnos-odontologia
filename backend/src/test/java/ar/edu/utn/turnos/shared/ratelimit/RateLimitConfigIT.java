package ar.edu.utn.turnos.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class RateLimitConfigIT extends PruebaIntegracionBase {

    @Autowired
    private RateLimitProperties propiedades;

    @Autowired
    private LimitadorPorClave limitadorPorClave;

    @Autowired
    private FilterRegistrationBean<FiltroLimiteTasaPublico> registroFiltro;

    @Test
    @DisplayName("El contexto enlaza las propiedades de app.ratelimit y registra el filtro público")
    void contextoEnlazaPropiedadesYRegistraFiltroPublico() {
        assertThat(propiedades.maxSolicitudesPorIp()).isEqualTo(60);
        assertThat(propiedades.ventanaIp()).isEqualTo(Duration.ofMinutes(1));
        assertThat(propiedades.maxSolicitudesPorClave()).isEqualTo(10);
        assertThat(propiedades.ventanaClave()).isEqualTo(Duration.ofMinutes(1));
        assertThat(limitadorPorClave).isNotNull();
        assertThat(registroFiltro.getUrlPatterns()).containsExactly("/api/public/*");
    }
}
