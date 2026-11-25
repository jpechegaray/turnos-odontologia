package ar.edu.utn.turnos.agenda.domain.evento;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventosReprogramacionTest {
    /** Lunes 10 de junio de 2030, 10:00 en Buenos Aires. */
    private static final Instant DIEZ = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant CREADO = Instant.parse("2030-06-01T12:00:00Z");
    private static final Instant AHORA = DIEZ.minus(Duration.ofHours(2));
    private static final Duration PLAZO = Duration.ofHours(24);

    private static Turno anterior() {
        return new Turno(50L, 1L, 2L, 3L, 4L, Intervalo.desde(DIEZ, 30), 30, EstadoTurno.PROGRAMADO,
                CanalTurno.INTERNO, null, false, false, null, null, null, null, 9L, CREADO, 0L);
    }

    private static Intervalo minutos(int desdeDiez, int duracion) {
        return Intervalo.desde(DIEZ.plus(Duration.ofMinutes(desdeDiez)), duracion);
    }

    @Test
    @DisplayName("El evento de reprogramación lleva los recursos y los intervalos anterior y nuevo")
    void turnoReprogramado() {
        var anterior = anterior();
        var nuevo = anterior.reprogramar(20L, 30L, minutos(120, 30), AHORA, PLAZO);

        var evento = TurnoReprogramado.de(anterior, nuevo);

        assertThat(evento.turnoId()).isEqualTo(50L);
        assertThat(evento.pacienteId()).isEqualTo(1L);
        assertThat(evento.profesionalAnteriorId()).isEqualTo(2L);
        assertThat(evento.sillonAnteriorId()).isEqualTo(3L);
        assertThat(evento.anterior()).isEqualTo(minutos(0, 30));
        assertThat(evento.profesionalNuevoId()).isEqualTo(20L);
        assertThat(evento.sillonNuevoId()).isEqualTo(30L);
        assertThat(evento.nuevo()).isEqualTo(minutos(120, 30));
        // Faltan 2 horas para el inicio original: es tardía.
        assertThat(evento.reprogramadoTarde()).isTrue();
    }

    @Test
    @DisplayName("Moverse dentro del propio intervalo libera solo lo que dejó")
    void moverseDentroDelIntervalo() {
        var anterior = anterior();
        var nuevo = anterior.reprogramar(2L, 3L, minutos(15, 30), AHORA, PLAZO);

        assertThat(HuecoLiberado.porReprogramacion(anterior, nuevo))
                .containsExactly(new HuecoLiberado(2L, 3L, minutos(0, 15), 50L));
    }

    @Test
    @DisplayName("Cambiar de profesional libera el intervalo completo")
    void cambiarDeProfesional() {
        var anterior = anterior();
        var nuevo = anterior.reprogramar(20L, 3L, minutos(0, 30), AHORA, PLAZO);

        assertThat(HuecoLiberado.porReprogramacion(anterior, nuevo))
                .containsExactly(new HuecoLiberado(2L, 3L, minutos(0, 30), 50L));
    }

    @Test
    @DisplayName("Si el turno nuevo cubre al anterior no queda hueco, y un recorte interior deja dos")
    void sinHuecoODosTramos() {
        var anterior = anterior();
        var alargado = anterior.reprogramar(2L, 30L, minutos(0, 60), AHORA, PLAZO);
        assertThat(HuecoLiberado.porReprogramacion(anterior, alargado)).isEmpty();

        var largo = new Turno(51L, 1L, 2L, 3L, 4L, minutos(0, 60), 60, EstadoTurno.PROGRAMADO, CanalTurno.INTERNO,
                null, false, false, null, null, null, null, 9L, CREADO, 0L);
        var recortado = largo.reprogramar(2L, 3L, minutos(15, 15), AHORA, PLAZO);
        assertThat(HuecoLiberado.porReprogramacion(largo, recortado)).containsExactly(
                new HuecoLiberado(2L, 3L, minutos(0, 15), 51L), new HuecoLiberado(2L, 3L, minutos(30, 30), 51L));
    }
}
