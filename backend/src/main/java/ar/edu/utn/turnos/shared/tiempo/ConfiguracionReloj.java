package ar.edu.utn.turnos.shared.tiempo;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fuente de la hora actual (DD-05). Toda regla temporal recibe este {@link Clock} por
 * constructor y nunca llama a {@code Instant.now()} directamente.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    public Clock reloj() {
        return Clock.system(ZonaHoraria.BUENOS_AIRES);
    }
}
