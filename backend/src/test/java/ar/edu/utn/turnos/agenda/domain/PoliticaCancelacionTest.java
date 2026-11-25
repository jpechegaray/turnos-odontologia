package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PoliticaCancelacionTest {
    // Miércoles 2030-06-12 10:00 en Buenos Aires (UTC-3).
    private static final Instant INICIO = Instant.parse("2030-06-12T13:00:00Z");
    private static final Duration PLAZO = Duration.ofHours(24);

    @Test
    @DisplayName("Cancelar a 24 horas exactas no es tardío")
    void aPlazoExactoNoEsTardia() {
        var martesDiez = INICIO.minus(Duration.ofHours(24));

        assertThat(PoliticaCancelacion.esTardia(INICIO, martesDiez, PLAZO)).isFalse();
    }

    @Test
    @DisplayName("Cancelar un minuto después del plazo es tardío")
    void unMinutoDespuesEsTardia() {
        var martesDiezYUno = INICIO.minus(Duration.ofHours(24)).plus(Duration.ofMinutes(1));

        assertThat(PoliticaCancelacion.esTardia(INICIO, martesDiezYUno, PLAZO)).isTrue();
    }

    @Test
    @DisplayName("Un milisegundo después del plazo ya es tardío")
    void unMilisegundoDespuesEsTardia() {
        var ahora = INICIO.minus(PLAZO).plusMillis(1);

        assertThat(PoliticaCancelacion.esTardia(INICIO, ahora, PLAZO)).isTrue();
    }

    @Test
    @DisplayName("Con plazo cero nunca es tardía antes del inicio")
    void plazoCero() {
        var unMinutoAntes = INICIO.minus(Duration.ofMinutes(1));

        assertThat(PoliticaCancelacion.esTardia(INICIO, unMinutoAntes, Duration.ZERO)).isFalse();
    }

    @Test
    @DisplayName("Con plazo de 48 horas, cancelar 30 horas antes es tardío y 48 horas antes no")
    void plazoDeCuarentaYOchoHoras() {
        var plazo = Duration.ofHours(48);

        assertThat(PoliticaCancelacion.esTardia(INICIO, INICIO.minus(Duration.ofHours(30)), plazo)).isTrue();
        assertThat(PoliticaCancelacion.esTardia(INICIO, INICIO.minus(Duration.ofHours(48)), plazo)).isFalse();
    }
}
