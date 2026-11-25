package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCreado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Clock;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Paso común de toda alta de turno (personal, C-19; online, C-42): bloquea profesional y sillón, aplica las reglas
 * puras de {@link ValidadorTurno}, guarda y publica {@link TurnoCreado}. Debe correr dentro de una transacción: los
 * locks y el rechazo de la base (RN-15) duran hasta el commit.
 */
@Component
class AltaTurno {
    private final TurnosPersistencia turnos;
    private final ConsultaRecursosTurno recursos;
    private final ConsultasAgenda consultas;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ValidadorTurno validador;

    AltaTurno(TurnosPersistencia turnos, ConsultaRecursosTurno recursos, ConsultasAgenda consultas,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.recursos = recursos;
        this.consultas = consultas;
        this.eventos = eventos;
        this.reloj = reloj;
        this.validador = new ValidadorTurno(reloj);
    }

    /**
     * @param fabrica arma el turno a guardar a partir del intervalo validado y del instante de creación
     */
    Turno registrar(SolicitudTurno solicitud, BiFunction<Intervalo, Instant, Turno> fabrica) {
        // RN-15: las altas sobre el mismo profesional o sillón se ordenan; la segunda valida contra la primera.
        turnos.bloquearRecursos(solicitud.profesionalId(), solicitud.sillonId());
        var intervalo = validador.validar(solicitud, recursos.de(solicitud), consultas, null);
        var turno = turnos.guardar(fabrica.apply(intervalo, Instant.now(reloj)));
        eventos.publishEvent(TurnoCreado.de(turno));
        return turno;
    }
}
