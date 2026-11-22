package ar.edu.utn.turnos.profesionales.horario.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FranjaLocalTest {

    @Test
    @DisplayName("Un candidato en UTC se convierte al día y las horas locales de Buenos Aires")
    void convierteABuenosAires() {
        // 2030-06-10 es lunes.
        var franja = FranjaLocal.de(intervalo("2030-06-10T12:00:00Z", "2030-06-10T12:30:00Z"));

        assertThat(franja).contains(new FranjaLocal(1, LocalTime.of(9, 0), LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("Un candidato que cruza la medianoche local no tiene franja")
    void cruzaMedianoche() {
        // Domingo 23:30 a lunes 00:30 en Buenos Aires.
        assertThat(FranjaLocal.de(intervalo("2030-06-10T02:30:00Z", "2030-06-10T03:30:00Z"))).isEmpty();
    }

    @Test
    @DisplayName("Un candidato que termina a las 00:00 del día siguiente no tiene franja")
    void terminaALaMedianoche() {
        // Lunes 23:30 a martes 00:00 en Buenos Aires.
        assertThat(FranjaLocal.de(intervalo("2030-06-11T02:30:00Z", "2030-06-11T03:00:00Z"))).isEmpty();
    }

    private static Intervalo intervalo(String inicio, String fin) {
        return new Intervalo(Instant.parse(inicio), Instant.parse(fin));
    }
}
