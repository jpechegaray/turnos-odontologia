package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCreado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
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
        // RN-15: las altas sobre el mismo profesional o sillón se ordenan; la segunda valida contra la primera.
        turnos.bloquearRecursos(solicitud.profesionalId(), solicitud.sillonId());
        var intervalo = validador.validar(solicitud, recursos.de(solicitud), consultas, null);
        var turno = turnos.guardar(Turno.nuevo(solicitud.pacienteId(), solicitud.profesionalId(), solicitud.sillonId(),
                solicitud.prestacionId(), intervalo, notas, creadoPorUsuarioId, Instant.now(reloj)));
        eventos.publishEvent(TurnoCreado.de(turno));
        return turno;
    }
}
