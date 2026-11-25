package ar.edu.utn.turnos.agenda.domain.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TurnoCreadoTest {
    private static final Instant INICIO = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant CREADO = Instant.parse("2030-06-01T12:00:00Z");

    @Test
    @DisplayName("El evento TurnoCreado copia el id, los recursos y el intervalo de un turno guardado")
    void deUnTurnoGuardado() {
        var intervalo = Intervalo.desde(INICIO, 30);
        var turno = new Turno(50L, 1L, 2L, 3L, 4L, intervalo, 30, EstadoTurno.PROGRAMADO, CanalTurno.INTERNO,
                null, false, false, null, null, null, null, 9L, CREADO, 0L);

        var evento = TurnoCreado.de(turno);

        assertThat(evento.turnoId()).isEqualTo(50L);
        assertThat(evento.pacienteId()).isEqualTo(1L);
        assertThat(evento.profesionalId()).isEqualTo(2L);
        assertThat(evento.sillonId()).isEqualTo(3L);
        assertThat(evento.intervalo()).isEqualTo(intervalo);
    }

    @Test
    @DisplayName("No se publica TurnoCreado de un turno que todavía no se guardó")
    void exigeTurnoGuardado() {
        var nuevo = Turno.nuevo(1L, 2L, 3L, 4L, Intervalo.desde(INICIO, 30), null, 9L, CREADO);
        assertThatThrownBy(() -> TurnoCreado.de(nuevo)).isInstanceOf(IllegalArgumentException.class);
    }
}
