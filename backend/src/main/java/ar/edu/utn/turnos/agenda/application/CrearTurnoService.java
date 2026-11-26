package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCreado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.shared.notificacion.application.EventosAvisoAlta;
import java.time.Clock;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea un turno dado por el personal (US-010, Flujo 1): carga los recursos con los puertos de los otros
 * módulos, aplica las reglas puras de {@link ValidadorTurno}, guarda y publica {@link TurnoCreado}. Si otra
 * transacción ocupó el hueco mientras tanto, la persistencia responde el mismo 409 (RN-15).
 */
@Service
public class CrearTurnoService {
    private final TurnosPersistencia turnos;
    private final ConsultaRecursosTurno recursos;
    private final ConsultasAgenda consultas;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ValidadorTurno validador;

    CrearTurnoService(TurnosPersistencia turnos, ConsultaRecursosTurno recursos, ConsultasAgenda consultas,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.recursos = recursos;
        this.consultas = consultas;
        this.eventos = eventos;
        this.reloj = reloj;
        this.validador = new ValidadorTurno(reloj);
    }

    /**
     * @param creadoPorUsuarioId usuario del personal que da el turno (trazabilidad); puede ser null
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno crear(SolicitudTurno solicitud, String notas, Long creadoPorUsuarioId) {
        return crearPersonal(solicitud, notas, creadoPorUsuarioId, false);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional(propagation = Propagation.MANDATORY)
    public Turno crearDesdeLista(SolicitudTurno solicitud, String notas, Long creadoPorUsuarioId) {
        return crearPersonal(solicitud, notas, creadoPorUsuarioId, true);
    }

    private Turno crearPersonal(SolicitudTurno solicitud, String notas, Long creadoPorUsuarioId, boolean desdeLista) {
        var turno = crearCon(solicitud, intervalo -> Turno.nuevo(solicitud.pacienteId(), solicitud.profesionalId(),
                solicitud.sillonId(), solicitud.prestacionId(), intervalo, notas, creadoPorUsuarioId, Instant.now(reloj)));
        eventos.publishEvent(new EventosAvisoAlta.TurnoAgendado(turno.id(), desdeLista));
        return turno;
    }

    /**
     * Alta de la reserva online (C-41, Flujo 4): las mismas reglas, bloqueos y garantía de la base que el alta del
     * personal, con canal ONLINE y sin usuario creador. No tiene autorización propia porque la pide un paciente
     * anónimo; por eso exige una transacción ya abierta ({@code MANDATORY}): solo la usa la reserva pública, que
     * valida antes el consentimiento y los límites de la reserva online.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Turno crearOnline(SolicitudTurno solicitud) {
        return crearCon(solicitud, intervalo -> Turno.nuevoOnline(solicitud.pacienteId(), solicitud.profesionalId(),
                solicitud.sillonId(), solicitud.prestacionId(), intervalo, Instant.now(reloj)));
    }

    private Turno crearCon(SolicitudTurno solicitud, Function<Intervalo, Turno> construir) {
        // RN-15: las altas sobre el mismo profesional o sillón se ordenan; la segunda valida contra la primera.
        turnos.bloquearRecursos(solicitud.profesionalId(), solicitud.sillonId());
        var intervalo = validador.validar(solicitud, recursos.de(solicitud), consultas, null);
        var turno = turnos.guardar(construir.apply(intervalo));
        eventos.publishEvent(TurnoCreado.de(turno));
        return turno;
    }
}
