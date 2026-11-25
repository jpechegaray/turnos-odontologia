package ar.edu.utn.turnos.agenda.domain.evento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventosCancelacionTest {
    private static final Instant INICIO = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant CREADO = Instant.parse("2030-06-01T12:00:00Z");
    private static final Instant AHORA = INICIO.minus(Duration.ofHours(2));

    private static Turno programado(Long id) {
        return new Turno(id, 1L, 2L, 3L, 4L, Intervalo.desde(INICIO, 30), 30, EstadoTurno.PROGRAMADO,
                CanalTurno.INTERNO, null, false, false, null, null, null, null, 9L, CREADO, 0L);
    }

    private static Turno cancelado() {
        return programado(50L).cancelar(ActorCancelacion.USUARIO, 15L, null, AHORA, Duration.ofHours(24));
    }

    @Test
    @DisplayName("TurnoCancelado copia el turno, la marca de tardía y el momento de la cancelación")
    void turnoCancelado() {
        var evento = TurnoCancelado.de(cancelado());

        assertThat(evento.turnoId()).isEqualTo(50L);
        assertThat(evento.pacienteId()).isEqualTo(1L);
        assertThat(evento.profesionalId()).isEqualTo(2L);
        assertThat(evento.sillonId()).isEqualTo(3L);
        assertThat(evento.intervalo()).isEqualTo(Intervalo.desde(INICIO, 30));
        assertThat(evento.cancelacionTardia()).isTrue();
        assertThat(evento.canceladoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("HuecoLiberado lleva el profesional, el sillón, el intervalo y el turno de origen")
    void huecoLiberado() {
        var evento = HuecoLiberado.de(cancelado());

        assertThat(evento.profesionalId()).isEqualTo(2L);
        assertThat(evento.sillonId()).isEqualTo(3L);
        assertThat(evento.hueco()).isEqualTo(Intervalo.desde(INICIO, 30));
        assertThat(evento.turnoOrigenId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Los eventos de cancelación exigen un turno guardado y cancelado")
    void exigenTurnoCanceladoYGuardado() {
        var sinGuardar = programado(null).cancelar(ActorCancelacion.USUARIO, 15L, null, AHORA, Duration.ofHours(24));

        assertThatThrownBy(() -> TurnoCancelado.de(programado(50L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HuecoLiberado.de(programado(50L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnoCancelado.de(sinGuardar)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HuecoLiberado.de(sinGuardar)).isInstanceOf(IllegalArgumentException.class);
    }
}
