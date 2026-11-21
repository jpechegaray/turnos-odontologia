package ar.edu.utn.turnos.shared.tiempo;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Reemplaza el reloj del sistema por uno fijo en las pruebas de integración que lo importan con
 * {@code @Import(RelojFijoConfig.class)}. Es {@code @Primary}, así que no hay que tocar producción.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RelojFijoConfig {

    public static final Instant INSTANTE_FIJO = Instant.parse("2026-12-01T12:00:00Z");

    @Bean
    @Primary
    Clock relojFijo() {
        return Clock.fixed(INSTANTE_FIJO, ZonaHoraria.BUENOS_AIRES);
    }
}
