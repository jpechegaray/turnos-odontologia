package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoReprogramado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reprograma el mismo turno (US-011, Flujo 2, RN-18): valida el horario nuevo con las reglas del alta excluyendo al
 * propio turno, marca la tardía (SU-07, PA-06) y publica {@link TurnoReprogramado} y un {@link HuecoLiberado} por
 * cada tramo que quedó libre. Locks en orden fijo: fila del turno, después profesional y sillón nuevos (design D4).
 */
@Service
public class ReprogramarTurnoService {
    private final TurnosPersistencia turnos;
    private final ConsultaRecursosTurno recursos;
    private final ConsultasAgenda consultas;
    private final ConfiguracionServicio configuracion;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ValidadorTurno validador;

    ReprogramarTurnoService(TurnosPersistencia turnos, ConsultaRecursosTurno recursos, ConsultasAgenda consultas,
            ConfiguracionServicio configuracion, ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.recursos = recursos;
        this.consultas = consultas;
        this.configuracion = configuracion;
        this.eventos = eventos;
        this.reloj = reloj;
        this.validador = new ValidadorTurno(reloj);
    }

    /** Horario pedido; profesional, sillón y duración nulos conservan los del turno. */
    public record Pedido(Instant inicio, Long profesionalId, Long sillonId, Integer duracionMin) {
        public Pedido {
            Objects.requireNonNull(inicio, "inicio");
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno reprogramar(Long turnoId, Pedido pedido) {
        var turno = turnos.buscarParaActualizar(turnoId).orElseThrow(() -> new ReglaNegocioException(
                "TURNO_INEXISTENTE", EstadoError.NO_ENCONTRADO_404, "No existe el turno " + turnoId));
        var ahora = Instant.now(reloj);
        // Antes de mirar el horario nuevo: un turno cancelado no informa "horario ocupado".
        turno.exigirReprogramable(ahora);

        var solicitud = new SolicitudTurno(turno.pacienteId(),
                pedido.profesionalId() != null ? pedido.profesionalId() : turno.profesionalId(),
                pedido.sillonId() != null ? pedido.sillonId() : turno.sillonId(), turno.prestacionId(),
                pedido.inicio(), pedido.duracionMin() != null ? pedido.duracionMin() : turno.duracionMin());
        turnos.bloquearRecursos(solicitud.profesionalId(), solicitud.sillonId());
        var intervalo = validador.validar(solicitud, recursos.de(solicitud), consultas, turno.id());
        if (turno.mismoHorario(solicitud.profesionalId(), solicitud.sillonId(), intervalo)) {
            return turno;
        }

        var plazo = Duration.ofHours(configuracion.plazoCancelacionHoras());
        var reprogramado = turnos.guardar(
                turno.reprogramar(solicitud.profesionalId(), solicitud.sillonId(), intervalo, ahora, plazo));
        eventos.publishEvent(TurnoReprogramado.de(turno, reprogramado));
        HuecoLiberado.porReprogramacion(turno, reprogramado).forEach(eventos::publishEvent);
        return reprogramado;
    }
}
