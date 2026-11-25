package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoReprogramado;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.ColaRecordatorios;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AvisosCambiosTurno {
    private final PacienteConsulta pacientes;
    private final ColaRecordatorios recordatorios;
    private final ServicioNotificaciones notificaciones;

    AvisosCambiosTurno(PacienteConsulta pacientes, ColaRecordatorios recordatorios,
            ServicioNotificaciones notificaciones) {
        this.pacientes = pacientes;
        this.recordatorios = recordatorios;
        this.notificaciones = notificaciones;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCancelar(TurnoCancelado evento) {
        recordatorios.descartarObsoletos(evento.turnoId());
        if (evento.actor() == ActorCancelacion.USUARIO) {
            avisar(TipoNotificacion.CANCELACION, evento.turnoId(), evento.pacienteId(), evento.intervalo().inicio());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alReprogramar(TurnoReprogramado evento) {
        recordatorios.descartarObsoletos(evento.turnoId());
        avisar(TipoNotificacion.REPROGRAMACION, evento.turnoId(), evento.pacienteId(), evento.nuevo().inicio());
    }

    private void avisar(TipoNotificacion tipo, Long turnoId, Long pacienteId, Instant inicio) {
        pacientes.porId(pacienteId).ifPresent(paciente -> notificaciones.registrarYEnviar(tipo, turnoId, null,
                pacienteId, new DatosNotificacion(paciente.email(), null, inicio, null, null, null)));
    }
}
