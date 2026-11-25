package ar.edu.utn.turnos.agenda.domain;

import static ar.edu.utn.turnos.agenda.domain.EstadoRecurso.ACTIVO;
import static ar.edu.utn.turnos.agenda.domain.EstadoRecurso.INACTIVO;
import static ar.edu.utn.turnos.agenda.domain.EstadoRecurso.INEXISTENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Reglas puras de creación de un turno (Flujo 1, paso 6). Sin Spring ni base de datos. */
class ValidadorTurnoTest {
    /** "Ahora" fijo: sábado 1 de junio de 2030, 09:00 en Buenos Aires. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2030-06-01T12:00:00Z"), ZoneOffset.UTC);
    private static final ValidadorTurno VALIDADOR = new ValidadorTurno(RELOJ);

    private static final Long PACIENTE = 1L;
    private static final Long PROFESIONAL = 2L;
    private static final Long SILLON = 3L;
    private static final Long PRESTACION = 4L;

    /** Lunes 10 de junio de 2030 a la hora de Buenos Aires indicada ("HH:mm"); BA es UTC-3. */
    static Instant lunes(String hora) {
        var partes = hora.split(":");
        return Instant.parse("2030-06-10T00:00:00Z")
                .plusSeconds((Integer.parseInt(partes[0]) + 3) * 3600L + Integer.parseInt(partes[1]) * 60L);
    }

    static SolicitudTurno solicitud(String hora, Integer duracion) {
        return new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION, lunes(hora), duracion);
    }

    static RecursosTurno recursosOk() {
        return new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, ACTIVO, 30, true);
    }

    /** Agenda falsa: por defecto todo libre y dentro de horario. Cuenta las consultas recibidas. */
    static class AgendaFalsa implements ConsultasAgenda {
        boolean dentroDeHorario = true;
        boolean enBloqueo = false;
        final List<TurnoOcupado> ocupados = new ArrayList<>();
        int consultas = 0;

        @Override
        public boolean dentroDeHorario(Long profesionalId, Intervalo intervalo) {
            consultas++;
            return dentroDeHorario;
        }

        @Override
        public boolean enBloqueo(Long profesionalId, Intervalo intervalo) {
            consultas++;
            return enBloqueo;
        }

        @Override
        public List<TurnoOcupado> turnosQueSolapan(Long profesionalId, Long sillonId, Intervalo intervalo) {
            consultas++;
            return ocupados;
        }
    }

    static void rechaza(ThrowingCallable accion, EstadoError estado, String codigo) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
            assertThat(e.estado()).isEqualTo(estado);
            assertThat(e.codigo()).isEqualTo(codigo);
        });
    }

    @Test
    @DisplayName("Una solicitud válida devuelve el intervalo efectivo del turno")
    void solicitudValida() {
        var intervalo = VALIDADOR.validar(solicitud("10:00", null), recursosOk(), new AgendaFalsa(), null);
        assertThat(intervalo).isEqualTo(new Intervalo(lunes("10:00"), lunes("10:30")));
    }

    @Nested
    @DisplayName("RN-11: recursos existentes, activos y compatibles")
    class Recursos {

        private void rechazaRecursos(RecursosTurno recursos, String codigo) {
            var agenda = new AgendaFalsa();
            rechaza(() -> VALIDADOR.validar(solicitud("10:00", null), recursos, agenda, null),
                    EstadoError.NO_PROCESABLE_422, codigo);
            assertThat(agenda.consultas).as("no consulta la agenda si falla un recurso").isZero();
        }

        @Test
        @DisplayName("Un recurso inexistente se rechaza con el código de ese recurso")
        void inexistentes() {
            rechazaRecursos(new RecursosTurno(INEXISTENTE, ACTIVO, ACTIVO, ACTIVO, 30, true), "PACIENTE_INEXISTENTE");
            rechazaRecursos(new RecursosTurno(ACTIVO, INEXISTENTE, ACTIVO, ACTIVO, 30, true), "PROFESIONAL_INEXISTENTE");
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, INEXISTENTE, ACTIVO, 30, true), "SILLON_INEXISTENTE");
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, INEXISTENTE, 30, true), "PRESTACION_INEXISTENTE");
        }

        @Test
        @DisplayName("Un recurso inactivo se rechaza con el código de ese recurso")
        void inactivos() {
            rechazaRecursos(new RecursosTurno(INACTIVO, ACTIVO, ACTIVO, ACTIVO, 30, true), "PACIENTE_INACTIVO");
            rechazaRecursos(new RecursosTurno(ACTIVO, INACTIVO, ACTIVO, ACTIVO, 30, true), "PROFESIONAL_INACTIVO");
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, INACTIVO, ACTIVO, 30, true), "SILLON_INACTIVO");
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, INACTIVO, 30, true), "PRESTACION_INACTIVA");
        }

        @Test
        @DisplayName("El profesional no realiza la prestación")
        void profesionalNoRealizaPrestacion() {
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, ACTIVO, 30, false), "RN-11");
        }

        @Test
        @DisplayName("Con varios recursos inválidos se informa el primero: paciente, profesional, sillón, prestación")
        void ordenDeRecursos() {
            rechazaRecursos(new RecursosTurno(ACTIVO, INACTIVO, INEXISTENTE, INACTIVO, 30, false), "PROFESIONAL_INACTIVO");
            rechazaRecursos(new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, INACTIVO, 30, false), "PRESTACION_INACTIVA");
        }
    }

    @Nested
    @DisplayName("RN-13 y RN-09: inicio futuro, grilla de 5 minutos y duración")
    class Tiempo {

        private void rechazaSolicitud(SolicitudTurno solicitud, String codigo) {
            var agenda = new AgendaFalsa();
            rechaza(() -> VALIDADOR.validar(solicitud, recursosOk(), agenda, null), EstadoError.NO_PROCESABLE_422, codigo);
            assertThat(agenda.consultas).as("no consulta la agenda").isZero();
        }

        @Test
        @DisplayName("Un turno en el pasado se rechaza")
        void pasado() {
            Instant ahora = RELOJ.instant();
            rechazaSolicitud(new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION, ahora.minusSeconds(300), null),
                    "RN-13");
            rechazaSolicitud(new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION, ahora, null), "RN-13");
        }

        @Test
        @DisplayName("Un turno cinco minutos después del momento actual se acepta")
        void justoDespuesDeAhora() {
            var inicio = RELOJ.instant().plusSeconds(300);
            var intervalo = VALIDADOR.validar(new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION, inicio, null),
                    recursosOk(), new AgendaFalsa(), null);
            assertThat(intervalo.inicio()).isEqualTo(inicio);
        }

        @Test
        @DisplayName("Un inicio fuera de la grilla de cinco minutos se rechaza")
        void inicioFueraDeGrilla() {
            rechazaSolicitud(solicitud("10:03", null), "RN-09");
            rechazaSolicitud(new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION,
                    lunes("10:00").plusSeconds(30), null), "RN-09");
        }

        @Test
        @DisplayName("Una duración fuera de rango se rechaza")
        void duracionFueraDeRango() {
            rechazaSolicitud(solicitud("10:00", 7), "RN-09");
            rechazaSolicitud(solicitud("10:00", 485), "RN-09");
            rechazaSolicitud(solicitud("10:00", 0), "RN-09");
        }

        @Test
        @DisplayName("Sin duración se usa la de la prestación")
        void duracionPorDefecto() {
            var recursos = new RecursosTurno(ACTIVO, ACTIVO, ACTIVO, ACTIVO, 45, true);
            var intervalo = VALIDADOR.validar(solicitud("10:00", null), recursos, new AgendaFalsa(), null);
            assertThat(intervalo).isEqualTo(new Intervalo(lunes("10:00"), lunes("10:45")));
        }

        @Test
        @DisplayName("La duración se puede ajustar")
        void duracionAjustada() {
            var intervalo = VALIDADOR.validar(solicitud("10:00", 60), recursosOk(), new AgendaFalsa(), null);
            assertThat(intervalo).isEqualTo(new Intervalo(lunes("10:00"), lunes("11:00")));
        }
    }

    @Nested
    @DisplayName("RN-04 y RN-16: horario de atención y bloqueos")
    class HorarioYBloqueos {

        private void rechazaConMotivo(AgendaFalsa agenda, String motivo) {
            assertThatThrownBy(() -> VALIDADOR.validar(solicitud("12:45", null), recursosOk(), agenda, null))
                    .isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
                        assertThat(e.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                        assertThat(e.codigo()).isEqualTo("RN-04");
                        assertThat(e.propiedades()).containsEntry("motivo", motivo);
                    });
        }

        @Test
        @DisplayName("Un turno que sale del horario se rechaza")
        void fueraDeHorario() {
            var agenda = new AgendaFalsa();
            agenda.dentroDeHorario = false;
            rechazaConMotivo(agenda, "FUERA_DE_HORARIO");
        }

        @Test
        @DisplayName("Un turno dentro de un bloqueo se rechaza")
        void enBloqueo() {
            var agenda = new AgendaFalsa();
            agenda.enBloqueo = true;
            rechazaConMotivo(agenda, "BLOQUEO");
        }

        @Test
        @DisplayName("Fuera de horario se informa antes que un bloqueo")
        void horarioAntesQueBloqueo() {
            var agenda = new AgendaFalsa();
            agenda.dentroDeHorario = false;
            agenda.enBloqueo = true;
            rechazaConMotivo(agenda, "FUERA_DE_HORARIO");
        }

        @Test
        @DisplayName("Al horario y a los bloqueos se les pregunta por el intervalo efectivo del turno")
        void consultaConElIntervaloEfectivo() {
            var preguntados = new ArrayList<Intervalo>();
            var agenda = new AgendaFalsa() {
                @Override
                public boolean dentroDeHorario(Long profesionalId, Intervalo intervalo) {
                    assertThat(profesionalId).isEqualTo(PROFESIONAL);
                    preguntados.add(intervalo);
                    return true;
                }

                @Override
                public boolean enBloqueo(Long profesionalId, Intervalo intervalo) {
                    assertThat(profesionalId).isEqualTo(PROFESIONAL);
                    preguntados.add(intervalo);
                    return false;
                }
            };
            VALIDADOR.validar(solicitud("12:30", null), recursosOk(), agenda, null);
            var esperado = new Intervalo(lunes("12:30"), lunes("13:00"));
            assertThat(preguntados).containsExactly(esperado, esperado);
        }
    }

    @Nested
    @DisplayName("RN-01, RN-02 y RN-10: sin solapamiento por profesional ni por sillón")
    class Solapamiento {
        private static final Long OTRO_PROFESIONAL = 20L;
        private static final Long OTRO_SILLON = 30L;

        private static TurnoOcupado ocupado(long id, Long profesional, Long sillon, String desde, String hasta,
                EstadoTurno estado) {
            return new TurnoOcupado(id, profesional, sillon, new Intervalo(lunes(desde), lunes(hasta)), estado);
        }

        /** Valida un turno de 10:00 a 10:30 contra los ocupados dados. */
        private Intervalo validarContra(Long turnoExcluidoId, TurnoOcupado... ocupados) {
            var agenda = new AgendaFalsa();
            agenda.ocupados.addAll(List.of(ocupados));
            return VALIDADOR.validar(solicitud("10:00", 30), recursosOk(), agenda, turnoExcluidoId);
        }

        private void rechazaPor(String recurso, TurnoOcupado... ocupados) {
            assertThatThrownBy(() -> validarContra(null, ocupados))
                    .isInstanceOfSatisfying(ReglaNegocioException.class, e -> {
                        assertThat(e.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                        assertThat(e.codigo()).isEqualTo("RN-01");
                        assertThat(e.propiedades()).containsEntry("recurso", recurso);
                    });
        }

        @Test
        @DisplayName("Un turno solapado por profesional se rechaza")
        void porProfesional() {
            rechazaPor("PROFESIONAL", ocupado(7, PROFESIONAL, OTRO_SILLON, "10:15", "10:45", EstadoTurno.PROGRAMADO));
        }

        @Test
        @DisplayName("Un turno solapado por sillón se rechaza")
        void porSillon() {
            rechazaPor("SILLON", ocupado(7, OTRO_PROFESIONAL, SILLON, "10:15", "10:45", EstadoTurno.CONFIRMADO));
        }

        @Test
        @DisplayName("Si choca por profesional y por sillón se informa el profesional")
        void ambosEjes() {
            rechazaPor("PROFESIONAL",
                    ocupado(7, OTRO_PROFESIONAL, SILLON, "10:00", "10:30", EstadoTurno.PROGRAMADO),
                    ocupado(8, PROFESIONAL, OTRO_SILLON, "10:00", "10:30", EstadoTurno.PROGRAMADO));
        }

        @Test
        @DisplayName("Un turno que empieza justo cuando termina otro no se solapa")
        void bordeContiguo() {
            var intervalo = validarContra(null,
                    ocupado(7, PROFESIONAL, SILLON, "09:30", "10:00", EstadoTurno.PROGRAMADO),
                    ocupado(8, PROFESIONAL, SILLON, "10:30", "11:00", EstadoTurno.PROGRAMADO));
            assertThat(intervalo).isEqualTo(new Intervalo(lunes("10:00"), lunes("10:30")));
        }

        @Test
        @DisplayName("Un turno que empieza un minuto antes del fin de otro se solapa")
        void unMinutoAntes() {
            rechazaPor("PROFESIONAL", ocupado(7, PROFESIONAL, OTRO_SILLON, "09:31", "10:01", EstadoTurno.PROGRAMADO));
        }

        @Test
        @DisplayName("Un turno cancelado no ocupa lugar")
        void canceladoNoOcupa() {
            validarContra(null, ocupado(7, PROFESIONAL, SILLON, "10:00", "10:30", EstadoTurno.CANCELADO));
        }

        @Test
        @DisplayName("Un turno atendido sigue ocupando lugar")
        void atendidoOcupa() {
            rechazaPor("PROFESIONAL", ocupado(7, PROFESIONAL, SILLON, "10:00", "10:30", EstadoTurno.ATENDIDO));
            rechazaPor("SILLON", ocupado(8, OTRO_PROFESIONAL, SILLON, "10:00", "10:30", EstadoTurno.AUSENTE));
        }

        @Test
        @DisplayName("Los turnos de otro profesional en otro sillón no molestan")
        void otrosRecursos() {
            validarContra(null, ocupado(7, OTRO_PROFESIONAL, OTRO_SILLON, "10:00", "10:30", EstadoTurno.PROGRAMADO));
        }

        @Test
        @DisplayName("El turno excluido no cuenta como ocupado (reprogramación, RN-18)")
        void turnoExcluido() {
            validarContra(7L, ocupado(7, PROFESIONAL, SILLON, "10:15", "10:45", EstadoTurno.PROGRAMADO));
            rechazaPor("PROFESIONAL", ocupado(7, PROFESIONAL, SILLON, "10:15", "10:45", EstadoTurno.PROGRAMADO));
        }
    }

    @Nested
    @DisplayName("Orden de las reglas (Flujo 1, paso 6)")
    class Orden {

        @Test
        @DisplayName("Con varias reglas incumplidas se informa la primera del Flujo 1")
        void primeraReglaIncumplida() {
            var agenda = new AgendaFalsa();
            agenda.dentroDeHorario = false;
            agenda.enBloqueo = true;
            agenda.ocupados.add(new TurnoOcupado(7L, PROFESIONAL, SILLON,
                    new Intervalo(lunes("10:00"), lunes("10:30")), EstadoTurno.PROGRAMADO));
            var enElPasadoYFueraDeGrilla = new SolicitudTurno(PACIENTE, PROFESIONAL, SILLON, PRESTACION,
                    RELOJ.instant().minusSeconds(180), 7);
            var inactivo = new RecursosTurno(ACTIVO, ACTIVO, INACTIVO, ACTIVO, 30, true);

            rechaza(() -> VALIDADOR.validar(enElPasadoYFueraDeGrilla, inactivo, agenda, null),
                    EstadoError.NO_PROCESABLE_422, "SILLON_INACTIVO");
            rechaza(() -> VALIDADOR.validar(enElPasadoYFueraDeGrilla, recursosOk(), agenda, null),
                    EstadoError.NO_PROCESABLE_422, "RN-13");
            rechaza(() -> VALIDADOR.validar(solicitud("10:03", 7), recursosOk(), agenda, null),
                    EstadoError.NO_PROCESABLE_422, "RN-09");
            rechaza(() -> VALIDADOR.validar(solicitud("10:00", 30), recursosOk(), agenda, null),
                    EstadoError.NO_PROCESABLE_422, "RN-04");
            agenda.dentroDeHorario = true;
            rechaza(() -> VALIDADOR.validar(solicitud("10:00", 30), recursosOk(), agenda, null),
                    EstadoError.NO_PROCESABLE_422, "RN-04");
            agenda.enBloqueo = false;
            rechaza(() -> VALIDADOR.validar(solicitud("10:00", 30), recursosOk(), agenda, null),
                    EstadoError.CONFLICTO_409, "RN-01");
        }
    }
}
