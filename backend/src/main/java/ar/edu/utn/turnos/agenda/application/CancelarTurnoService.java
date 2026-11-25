package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancela un turno y libera su intervalo (US-012, Flujo 3; RN-05, RN-06, RN-10, RN-19). Lee el turno con lock
 * de fila: una segunda cancelación simultánea espera, ve el turno ya cancelado y recibe 409, así
 * {@link HuecoLiberado} se publica una sola vez. Los oyentes (C-36) deben escuchar después del commit.
 */
@Service
public class CancelarTurnoService {
    private final TurnosPersistencia turnos;
    private final ConfiguracionServicio configuracion;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public CancelarTurnoService(TurnosPersistencia turnos, ConfiguracionServicio configuracion,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.configuracion = configuracion;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Cancelación por el personal (RN-20, RN-33: el odontólogo no cancela). */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno cancelarPorPersonal(Long turnoId, String motivo, Long usuarioId) {
        return cancelar(turnoId, ActorCancelacion.USUARIO, usuarioId, motivo);
    }

    /**
     * Núcleo común. El actor {@code PACIENTE_TOKEN} lo usará la cancelación por token (C-32/C-39), que valida
     * el token antes de llamar; C-27 no lo expone por HTTP.
     */
    @Transactional
    public Turno cancelar(Long turnoId, ActorCancelacion actor, Long usuarioId, String motivo) {
        var turno = turnos.buscarParaActualizar(turnoId).orElseThrow(() -> new ReglaNegocioException(
                "TURNO_INEXISTENTE", EstadoError.NO_ENCONTRADO_404, "No existe el turno " + turnoId));
        var plazo = Duration.ofHours(configuracion.plazoCancelacionHoras());
        var cancelado = turnos.guardar(turno.cancelar(actor, usuarioId, motivo, Instant.now(reloj), plazo));
        eventos.publishEvent(TurnoCancelado.de(cancelado));
        eventos.publishEvent(HuecoLiberado.de(cancelado));
        return cancelado;
    }
}
