package ar.edu.utn.turnos.shared.tiempo;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(RelojFijoConfig.class)
class RelojFijoIT extends PruebaIntegracionBase {

    /** Componente de prueba que depende de la hora actual, como lo haría una regla temporal. */
    static class LectorDeHora {
        private final Clock reloj;

        LectorDeHora(Clock reloj) {
            this.reloj = reloj;
        }

        Instant ahora() {
            return Instant.now(reloj);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracionLector {
        @Bean
        LectorDeHora lectorDeHora(Clock reloj) {
            return new LectorDeHora(reloj);
        }
    }

    @Autowired
    private LectorDeHora lector;

    @Test
    @DisplayName("Una prueba fija la hora actual del sistema")
    void pruebaFijaLaHoraActualDelSistema() {
        assertThat(lector.ahora()).isEqualTo(Instant.parse("2026-12-01T12:00:00Z"));
    }
}
