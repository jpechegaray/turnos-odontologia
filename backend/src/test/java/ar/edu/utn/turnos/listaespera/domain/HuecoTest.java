package ar.edu.utn.turnos.listaespera.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HuecoTest {
    private static final Instant INICIO = Instant.parse("2030-06-12T13:00:00Z");

    @Test
    @DisplayName("Un hueco expone su duración")
    void duracion() {
        var hueco = new Hueco(1L, INICIO, INICIO.plus(Duration.ofMinutes(30)));

        assertThat(hueco.duracion()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("Un hueco exige profesional, inicio y fin")
    void rechazaNulos() {
        var fin = INICIO.plusSeconds(60);
        assertThatThrownBy(() -> new Hueco(null, INICIO, fin)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Hueco(1L, null, fin)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Hueco(1L, INICIO, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("El fin de un hueco debe ser posterior al inicio")
    void finPosterior() {
        assertThatThrownBy(() -> new Hueco(1L, INICIO, INICIO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Hueco(1L, INICIO, INICIO.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
