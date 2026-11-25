package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
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

    @Test
    @DisplayName("Un turno online nuevo queda PROGRAMADO, con canal ONLINE y sin usuario creador")
    void turnoOnlineNuevo() {
        var turno = Turno.nuevoOnline(1L, 2L, 3L, 4L, Intervalo.desde(INICIO, 45), CREADO);
        assertThat(turno.id()).isNull();
        assertThat(turno.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
        assertThat(turno.canal()).isEqualTo(CanalTurno.ONLINE);
        assertThat(turno.duracionMin()).isEqualTo(45);
        assertThat(turno.creadoPorUsuarioId()).isNull();
        assertThat(turno.notas()).isNull();
        assertThat(turno.creadoEn()).isEqualTo(CREADO);
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
                EstadoTurno.PROGRAMADO, CanalTurno.INTERNO, null, false, false, null, null, null, null, 9L, CREADO, null));
        assertThat(turno.notas()).isEqualTo("Control ficticio");
    }

    // ---------- C-27: cancelar ----------

    private static final Duration PLAZO = Duration.ofHours(24);

    private static Turno guardado(EstadoTurno estado) {
        var turno = new Turno(70L, 1L, 2L, 3L, 4L, Intervalo.desde(INICIO, 30), 30, EstadoTurno.PROGRAMADO,
                CanalTurno.INTERNO, "Control ficticio", false, false, null, null, null, null, 9L, CREADO, 3L);
        return estado == EstadoTurno.PROGRAMADO ? turno : turno.pasarA(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO"})
    @DisplayName("Cancelar un turno vigente lo deja CANCELADO con quién, cuándo, motivo y marca de tardía")
    void cancelarTurnoVigente(EstadoTurno origen) {
        var ahora = INICIO.minus(Duration.ofHours(72));

        var cancelado = guardado(origen).cancelar(ActorCancelacion.USUARIO, 15L, "Aviso del paciente", ahora, PLAZO);

        assertThat(cancelado.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(cancelado.canceladoEn()).isEqualTo(ahora);
        assertThat(cancelado.canceladoPor()).isEqualTo(ActorCancelacion.USUARIO);
        assertThat(cancelado.canceladoPorUsuarioId()).isEqualTo(15L);
        assertThat(cancelado.motivoCancelacion()).isEqualTo("Aviso del paciente");
        assertThat(cancelado.cancelacionTardia()).isFalse();
        assertThat(cancelado.id()).isEqualTo(70L);
        assertThat(cancelado.version()).isEqualTo(3L);
        assertThat(cancelado.intervalo()).isEqualTo(guardado(origen).intervalo());
    }

    @Test
    @DisplayName("Cancelar dentro del plazo queda marcado como tardía y el paciente por token no lleva usuario")
    void cancelacionTardiaPorToken() {
        var ahora = INICIO.minus(Duration.ofHours(2));

        var cancelado = guardado(EstadoTurno.PROGRAMADO).cancelar(ActorCancelacion.PACIENTE_TOKEN, null, null, ahora,
                PLAZO);

        assertThat(cancelado.cancelacionTardia()).isTrue();
        assertThat(cancelado.canceladoPor()).isEqualTo(ActorCancelacion.PACIENTE_TOKEN);
        assertThat(cancelado.canceladoPorUsuarioId()).isNull();
        assertThat(cancelado.motivoCancelacion()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Un turno en estado final no se cancela: 409 TURNO_TRANSICION_INVALIDA")
    void estadoFinalNoSeCancela(EstadoTurno origen) {
        var ahora = INICIO.minus(Duration.ofHours(72));
        var turno = guardado(origen);

        assertThatThrownBy(() -> turno.cancelar(ActorCancelacion.USUARIO, 15L, null, ahora, PLAZO))
                .satisfies(TurnoTest::esTransicionInvalida);
    }

    @Test
    @DisplayName("Un turno que empieza justo ahora o ya empezó no se cancela: 422 RN-05")
    void turnoIniciadoNoSeCancela() {
        var turno = guardado(EstadoTurno.PROGRAMADO);

        for (var ahora : new Instant[] {INICIO, INICIO.plusSeconds(60)}) {
            assertThatThrownBy(() -> turno.cancelar(ActorCancelacion.USUARIO, 15L, null, ahora, PLAZO))
                    .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                        assertThat(error.codigo()).isEqualTo("RN-05");
                        assertThat(error.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                    });
        }
    }

    @Test
    @DisplayName("Un turno atendido en el pasado responde la transición inválida, no RN-05")
    void atendidoEnElPasadoEsTransicionInvalida() {
        var turno = guardado(EstadoTurno.ATENDIDO);

        assertThatThrownBy(() -> turno.cancelar(ActorCancelacion.USUARIO, 15L, null, INICIO.plusSeconds(3600), PLAZO))
                .satisfies(TurnoTest::esTransicionInvalida);
    }

    // ---------- C-29: marcar y corregir asistencia ----------

    private static final Instant PASADO = INICIO.plus(Duration.ofHours(1));

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO"})
    @DisplayName("Marcar la asistencia de un turno vigente que ya empezó lo deja ATENDIDO")
    void marcarAtendidoDesdeVigente(EstadoTurno origen) {
        var turno = guardado(origen);

        var marcado = turno.marcarAsistencia(EstadoTurno.ATENDIDO, PASADO);

        assertThat(marcado.estado()).isEqualTo(EstadoTurno.ATENDIDO);
        assertThat(turno.estado()).isEqualTo(origen);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO"})
    @DisplayName("Marcar la ausencia de un turno vigente que ya empezó lo deja AUSENTE")
    void marcarAusenteDesdeVigente(EstadoTurno origen) {
        var marcado = guardado(origen).marcarAsistencia(EstadoTurno.AUSENTE, PASADO);

        assertThat(marcado.estado()).isEqualTo(EstadoTurno.AUSENTE);
    }

    @Test
    @DisplayName("A la hora exacta de inicio ya se puede marcar")
    void marcarALaHoraExacta() {
        var marcado = guardado(EstadoTurno.PROGRAMADO).marcarAsistencia(EstadoTurno.ATENDIDO, INICIO);

        assertThat(marcado.estado()).isEqualTo(EstadoTurno.ATENDIDO);
    }

    @Test
    @DisplayName("Antes del inicio no se puede marcar")
    void antesDelInicioNoSeMarca() {
        var turno = guardado(EstadoTurno.PROGRAMADO);

        assertThatThrownBy(() -> turno.marcarAsistencia(EstadoTurno.AUSENTE, INICIO.minusSeconds(60)))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("RN-20");
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });
        assertThat(turno.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
    }

    @Test
    @DisplayName("Un turno cancelado no se marca aunque su inicio ya pasó: 409 TURNO_TRANSICION_INVALIDA")
    void canceladoNoSeMarca() {
        var turno = guardado(EstadoTurno.CANCELADO);

        assertThatThrownBy(() -> turno.marcarAsistencia(EstadoTurno.ATENDIDO, PASADO))
                .satisfies(TurnoTest::esTransicionInvalida);
    }

    @Test
    @DisplayName("Marcar de nuevo un turno atendido en el pasado responde 409, no RN-20")
    void atendidoEnElPasadoNoSeVuelveAMarcar() {
        var turno = guardado(EstadoTurno.ATENDIDO);

        assertThatThrownBy(() -> turno.marcarAsistencia(EstadoTurno.AUSENTE, PASADO))
                .satisfies(TurnoTest::esTransicionInvalida);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO", "CANCELADO"})
    @DisplayName("Marcar asistencia exige un destino ATENDIDO o AUSENTE")
    void destinoInvalidoSeRechaza(EstadoTurno destino) {
        var turno = guardado(EstadoTurno.PROGRAMADO);

        assertThatIllegalArgumentException().isThrownBy(() -> turno.marcarAsistencia(destino, PASADO));
    }

    @Test
    @DisplayName("Marcar asistencia no cambia el intervalo, los recursos ni las notas")
    void marcarNoCambiaLoDemas() {
        var turno = guardado(EstadoTurno.CONFIRMADO);

        var marcado = turno.marcarAsistencia(EstadoTurno.ATENDIDO, PASADO);

        assertThat(marcado).usingRecursiveComparison().ignoringFields("estado").isEqualTo(turno);
    }

    @Test
    @DisplayName("La corrección pasa de ATENDIDO a AUSENTE y de AUSENTE a ATENDIDO sin mirar el reloj")
    void correccionEntreMarcas() {
        assertThat(guardado(EstadoTurno.ATENDIDO).corregirAsistencia(EstadoTurno.AUSENTE).estado())
                .isEqualTo(EstadoTurno.AUSENTE);
        assertThat(guardado(EstadoTurno.AUSENTE).corregirAsistencia(EstadoTurno.ATENDIDO).estado())
                .isEqualTo(EstadoTurno.ATENDIDO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"ATENDIDO", "AUSENTE"})
    @DisplayName("Repetir la misma marca se rechaza")
    void repetirMismaMarca(EstadoTurno marca) {
        var turno = guardado(marca);

        assertThatThrownBy(() -> turno.corregirAsistencia(marca)).satisfies(TurnoTest::esTransicionInvalida);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO", "CANCELADO"})
    @DisplayName("Solo un turno ATENDIDO o AUSENTE admite corrección: 409 TURNO_TRANSICION_INVALIDA")
    void correccionDesdeEstadoNoMarcado(EstadoTurno origen) {
        var turno = guardado(origen);

        for (var destino : new EstadoTurno[] {EstadoTurno.ATENDIDO, EstadoTurno.AUSENTE}) {
            assertThatThrownBy(() -> turno.corregirAsistencia(destino)).satisfies(TurnoTest::esTransicionInvalida);
        }
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO", "CANCELADO"})
    @DisplayName("La corrección solo lleva a la otra marca de asistencia: 409 TURNO_TRANSICION_INVALIDA")
    void correccionADestinoInvalido(EstadoTurno destino) {
        var turno = guardado(EstadoTurno.ATENDIDO);

        assertThatThrownBy(() -> turno.corregirAsistencia(destino)).satisfies(TurnoTest::esTransicionInvalida);
    }

    @Test
    @DisplayName("La corrección no cambia lo demás")
    void correccionNoCambiaLoDemas() {
        var turno = guardado(EstadoTurno.AUSENTE);

        var corregido = turno.corregirAsistencia(EstadoTurno.ATENDIDO);

        assertThat(corregido.estado()).isEqualTo(EstadoTurno.ATENDIDO);
        assertThat(corregido).usingRecursiveComparison().ignoringFields("estado").isEqualTo(turno);
    }

    @Test
    @DisplayName("La corrección de asistencia no abre otras transiciones")
    void correccionNoAbreOtrasTransiciones() {
        var corregido = guardado(EstadoTurno.ATENDIDO).corregirAsistencia(EstadoTurno.AUSENTE);

        assertThatThrownBy(() -> corregido.cancelar(ActorCancelacion.USUARIO, 15L, null,
                INICIO.minus(Duration.ofHours(72)), PLAZO)).satisfies(TurnoTest::esTransicionInvalida);
        assertThatThrownBy(() -> corregido.pasarA(EstadoTurno.CONFIRMADO)).satisfies(TurnoTest::esTransicionInvalida);
    }

    private static void esTransicionInvalida(Throwable error) {
        assertThat(error).isInstanceOf(ReglaNegocioException.class);
        var regla = (ReglaNegocioException) error;
        assertThat(regla.codigo()).isEqualTo("TURNO_TRANSICION_INVALIDA");
        assertThat(regla.estado()).isEqualTo(EstadoError.CONFLICTO_409);
    }

    // ---------- C-35: reprogramar ----------

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Un turno en estado final no se reprograma: 409 TURNO_TRANSICION_INVALIDA")
    void estadoFinalNoSeReprograma(EstadoTurno origen) {
        var turno = guardado(origen);
        var ahora = INICIO.minus(Duration.ofHours(72));

        assertThatThrownBy(() -> turno.exigirReprogramable(ahora)).satisfies(TurnoTest::esTransicionInvalida);
        // Aunque ya haya pasado, el estado final se informa primero.
        assertThatThrownBy(() -> turno.exigirReprogramable(INICIO.plusSeconds(3600)))
                .satisfies(TurnoTest::esTransicionInvalida);
    }

    @Test
    @DisplayName("Un turno que empieza justo ahora o ya empezó no se reprograma: 422 RN-05")
    void turnoIniciadoNoSeReprograma() {
        var turno = guardado(EstadoTurno.PROGRAMADO);

        for (var ahora : new Instant[] {INICIO, INICIO.plusSeconds(60)}) {
            assertThatThrownBy(() -> turno.reprogramar(2L, 3L, Intervalo.desde(INICIO.plusSeconds(7200), 30), ahora,
                    PLAZO))
                    .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                        assertThat(error.codigo()).isEqualTo("RN-05");
                        assertThat(error.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                    });
        }
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"PROGRAMADO", "CONFIRMADO"})
    @DisplayName("Reprogramar cambia recursos e intervalo y conserva la identidad del turno")
    void reprogramarConservaIdentidad(EstadoTurno origen) {
        var ahora = INICIO.minus(Duration.ofHours(72));
        var nuevo = Intervalo.desde(INICIO.plus(Duration.ofDays(1)), 45);

        var reprogramado = guardado(origen).reprogramar(20L, 30L, nuevo, ahora, PLAZO);

        assertThat(reprogramado.id()).isEqualTo(70L);
        assertThat(reprogramado.estado()).isEqualTo(origen);
        assertThat(reprogramado.profesionalId()).isEqualTo(20L);
        assertThat(reprogramado.sillonId()).isEqualTo(30L);
        assertThat(reprogramado.intervalo()).isEqualTo(nuevo);
        assertThat(reprogramado.duracionMin()).isEqualTo(45);
        assertThat(reprogramado.pacienteId()).isEqualTo(1L);
        assertThat(reprogramado.prestacionId()).isEqualTo(4L);
        assertThat(reprogramado.notas()).isEqualTo("Control ficticio");
        assertThat(reprogramado.version()).isEqualTo(3L);
        assertThat(reprogramado.reprogramadoTarde()).isFalse();
    }

    @Test
    @DisplayName("La reprogramación es tardía si falta menos que el plazo para el inicio original")
    void reprogramacionTardia() {
        var nuevo = Intervalo.desde(INICIO.plus(Duration.ofDays(3)), 30);
        var turno = guardado(EstadoTurno.PROGRAMADO);

        assertThat(turno.reprogramar(2L, 3L, nuevo, INICIO.minus(PLAZO), PLAZO).reprogramadoTarde()).isFalse();
        assertThat(turno.reprogramar(2L, 3L, nuevo, INICIO.minus(PLAZO).plusSeconds(60), PLAZO).reprogramadoTarde())
                .isTrue();
    }

    @Test
    @DisplayName("Una reprogramación tardía queda marcada aunque la siguiente sea a tiempo")
    void marcaTardiaSeConserva() {
        var tarde = guardado(EstadoTurno.PROGRAMADO)
                .reprogramar(2L, 3L, Intervalo.desde(INICIO.plus(Duration.ofDays(5)), 30), INICIO.minusSeconds(3600),
                        PLAZO);
        var otraVez = tarde.reprogramar(2L, 3L, Intervalo.desde(INICIO.plus(Duration.ofDays(6)), 30),
                INICIO.minusSeconds(3600), PLAZO);

        assertThat(tarde.reprogramadoTarde()).isTrue();
        assertThat(otraVez.reprogramadoTarde()).isTrue();
    }

    @Test
    @DisplayName("Mismo horario solo si coinciden profesional, sillón e intervalo")
    void mismoHorario() {
        var turno = guardado(EstadoTurno.PROGRAMADO);

        assertThat(turno.mismoHorario(2L, 3L, Intervalo.desde(INICIO, 30))).isTrue();
        assertThat(turno.mismoHorario(5L, 3L, Intervalo.desde(INICIO, 30))).isFalse();
        assertThat(turno.mismoHorario(2L, 5L, Intervalo.desde(INICIO, 30))).isFalse();
        assertThat(turno.mismoHorario(2L, 3L, Intervalo.desde(INICIO, 45))).isFalse();
        assertThat(turno.mismoHorario(2L, 3L, Intervalo.desde(INICIO.plusSeconds(300), 30))).isFalse();
    }
}
