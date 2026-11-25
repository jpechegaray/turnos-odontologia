package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TurnoTest {
    private static final Instant INICIO = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant CREADO = Instant.parse("2030-06-01T12:00:00Z");

    private static Turno nuevo() {
        return Turno.nuevo(1L, 2L, 3L, 4L, Intervalo.desde(INICIO, 30), "Control ficticio", 9L, CREADO);
    }

    @Test
    @DisplayName("Un turno nuevo queda PROGRAMADO, con canal INTERNO y su duración efectiva")
    void turnoNuevo() {
        var turno = nuevo();
        assertThat(turno.id()).isNull();
        assertThat(turno.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
        assertThat(turno.canal()).isEqualTo(CanalTurno.INTERNO);
        assertThat(turno.duracionMin()).isEqualTo(30);
        assertThat(turno.intervalo().fin()).isEqualTo(INICIO.plusSeconds(30 * 60));
        assertThat(turno.creadoPorUsuarioId()).isEqualTo(9L);
        assertThat(turno.creadoEn()).isEqualTo(CREADO);
        assertThat(turno.cancelacionTardia()).isFalse();
        assertThat(turno.reprogramadoTarde()).isFalse();
        assertThat(turno.canceladoEn()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CONFIRMADO", "CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Transiciones permitidas desde un turno programado")
    void transicionesDesdeProgramado(EstadoTurno destino) {
        var turno = nuevo();
        var siguiente = turno.pasarA(destino);
        assertThat(siguiente.estado()).isEqualTo(destino);
        // Las transiciones no mutan el turno original.
        assertThat(turno.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Un turno confirmado puede cancelarse o marcarse como atendido o ausente")
    void transicionesDesdeConfirmado(EstadoTurno destino) {
        var confirmado = nuevo().pasarA(EstadoTurno.CONFIRMADO);
        assertThat(confirmado.pasarA(destino).estado()).isEqualTo(destino);
    }

    @Test
    @DisplayName("Un turno confirmado no vuelve a programado ni se confirma dos veces")
    void confirmadoNoRetrocede() {
        var confirmado = nuevo().pasarA(EstadoTurno.CONFIRMADO);
        assertThatThrownBy(() -> confirmado.pasarA(EstadoTurno.PROGRAMADO)).satisfies(TurnoTest::esTransicionInvalida);
        assertThatThrownBy(() -> confirmado.pasarA(EstadoTurno.CONFIRMADO)).satisfies(TurnoTest::esTransicionInvalida);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Un estado final no cambia")
    void estadoFinalNoCambia(EstadoTurno estadoFinal) {
        var turno = nuevo().pasarA(estadoFinal);
        assertThat(estadoFinal.esFinal()).isTrue();
        for (EstadoTurno destino : EstadoTurno.values()) {
            assertThatThrownBy(() -> turno.pasarA(destino)).satisfies(TurnoTest::esTransicionInvalida);
        }
    }

    @Test
    @DisplayName("Solo los turnos no cancelados ocupan lugar en la agenda")
    void ocupaLugar() {
        assertThat(EstadoTurno.CANCELADO.ocupaLugar()).isFalse();
        assertThat(EstadoTurno.PROGRAMADO.ocupaLugar()).isTrue();
        assertThat(EstadoTurno.CONFIRMADO.ocupaLugar()).isTrue();
        assertThat(EstadoTurno.ATENDIDO.ocupaLugar()).isTrue();
        assertThat(EstadoTurno.AUSENTE.ocupaLugar()).isTrue();
    }

    @Test
    @DisplayName("Un turno exige sus cuatro recursos y una duración que coincide con su intervalo")
    void invariantes() {
        var intervalo = Intervalo.desde(INICIO, 30);
        assertThatThrownBy(() -> Turno.nuevo(null, 2L, 3L, 4L, intervalo, null, 9L, CREADO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Turno.nuevo(1L, 2L, null, 4L, intervalo, null, 9L, CREADO))
                .isInstanceOf(NullPointerException.class);
        var turno = nuevo();
        assertThatIllegalArgumentException().isThrownBy(() -> new Turno(null, 1L, 2L, 3L, 4L, intervalo, 45,
                EstadoTurno.PROGRAMADO, CanalTurno.INTERNO, null, false, false, null, null, null, 9L, CREADO, null));
        assertThat(turno.notas()).isEqualTo("Control ficticio");
    }

    private static void esTransicionInvalida(Throwable error) {
        assertThat(error).isInstanceOf(ReglaNegocioException.class);
        var regla = (ReglaNegocioException) error;
        assertThat(regla.codigo()).isEqualTo("TURNO_TRANSICION_INVALIDA");
        assertThat(regla.estado()).isEqualTo(EstadoError.CONFLICTO_409);
    }
}
