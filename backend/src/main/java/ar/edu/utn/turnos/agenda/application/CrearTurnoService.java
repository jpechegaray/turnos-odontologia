package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea un turno dado por el personal (US-010, Flujo 1). El bloqueo, la validación, el guardado y el evento
 * {@code TurnoCreado} los hace {@link AltaTurno}, el mismo paso que usa el alta online. Si otra transacción ocupó
 * el hueco mientras tanto, la persistencia responde el mismo 409 (RN-15).
 */
@Service
public class CrearTurnoService {
    private final AltaTurno alta;

    CrearTurnoService(AltaTurno alta) {
        this.alta = alta;
    }

    /**
     * @param creadoPorUsuarioId usuario del personal que da el turno (trazabilidad); puede ser null
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno crear(SolicitudTurno solicitud, String notas, Long creadoPorUsuarioId) {
        return alta.registrar(solicitud, (intervalo, ahora) -> Turno.nuevo(solicitud.pacienteId(),
                solicitud.profesionalId(), solicitud.sillonId(), solicitud.prestacionId(), intervalo, notas,
                creadoPorUsuarioId, ahora));
    }
}
