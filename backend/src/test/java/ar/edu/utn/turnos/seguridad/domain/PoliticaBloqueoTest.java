package ar.edu.utn.turnos.seguridad.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PoliticaBloqueoTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant VENCE = AHORA.plus(Duration.ofMinutes(15));

    @Test
    @DisplayName("El quinto fallo bloquea hasta el vencimiento exacto")
    void bloqueaEnElQuintoFallo() {
        var estado = politica(AHORA).registrarFallo(new PoliticaBloqueo.Estado(4, null));

        assertThat(estado.bloqueadoHasta()).isEqualTo(VENCE);
        assertThat(politica(VENCE.minusMillis(1)).estaBloqueado(estado)).isTrue();
        assertThat(politica(VENCE).estaBloqueado(estado)).isFalse();
        assertThat(politica(VENCE.minusSeconds(1)).registrarFallo(estado)).isEqualTo(estado);
    }

    @Test
    @DisplayName("Un ingreso correcto reinicia los intentos consecutivos")
    void reiniciaAlIngresar() {
        var politica = politica(AHORA);
        var reiniciado = politica.registrarExito();

        assertThat(politica.registrarFallo(reiniciado)).isEqualTo(new PoliticaBloqueo.Estado(1, null));
    }

    @Test
    @DisplayName("Después del vencimiento se inicia una nueva serie")
    void reiniciaAlVencer() {
        var estado = politica(VENCE).registrarFallo(new PoliticaBloqueo.Estado(5, VENCE));

        assertThat(estado).isEqualTo(new PoliticaBloqueo.Estado(1, null));
    }

    @Test
    @DisplayName("Los primeros cuatro fallos no bloquean la cuenta")
    void cuatroFallosNoBloquean() {
        var politica = politica(AHORA);
        var estado = politica.registrarFallo(new PoliticaBloqueo.Estado(3, null));

        assertThat(estado).isEqualTo(new PoliticaBloqueo.Estado(4, null));
        assertThat(politica.estaBloqueado(estado)).isFalse();
    }

    private PoliticaBloqueo politica(Instant ahora) {
        return new PoliticaBloqueo(Clock.fixed(ahora, ZoneOffset.UTC));
    }
}
