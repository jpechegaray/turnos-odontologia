package ar.edu.utn.turnos.shared.tiempo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfiguracionRelojTest {

    private final Clock reloj = new ConfiguracionReloj().reloj();

    @Test
    @DisplayName("El reloj del sistema usa la zona de Buenos Aires")
    void relojUsaZonaDeBuenosAires() {
        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
        assertThat(reloj.getZone().getId()).isEqualTo("America/Argentina/Buenos_Aires");
        assertThat(reloj.getZone()).isNotInstanceOf(ZoneOffset.class);
        assertThat(ZonaHoraria.BUENOS_AIRES).isEqualTo(reloj.getZone());
    }

    @Test
    @DisplayName("Un instante UTC se interpreta en hora local de Buenos Aires")
    void instanteUtcSeInterpretaEnHoraLocalDeBuenosAires() {
        Clock fijo = Clock.fixed(Instant.parse("2026-12-01T12:00:00Z"), reloj.getZone());

        assertThat(LocalDateTime.now(fijo)).isEqualTo(LocalDateTime.parse("2026-12-01T09:00"));
    }

    @Test
    @DisplayName("Un instante UTC de otra fecha se interpreta en hora local de Buenos Aires")
    void instanteUtcDeMadrugadaCaeEnElDiaAnterior() {
        Clock fijo = Clock.fixed(Instant.parse("2026-07-01T02:30:00Z"), reloj.getZone());

        assertThat(LocalDateTime.now(fijo)).isEqualTo(LocalDateTime.parse("2026-06-30T23:30"));
    }
}
