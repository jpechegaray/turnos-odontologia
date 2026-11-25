package ar.edu.utn.turnos.reservapublica.token;

import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoReprogramado;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** La agenda conserva el bloqueo de la fila del turno durante estos cambios. */
@Component
class EventosTokenTurno {
    private final TokenTurnoRepositorio tokens;

    EventosTokenTurno(TokenTurnoRepositorio tokens) {
        this.tokens = tokens;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void alCancelar(TurnoCancelado evento) {
        tokens.revocar(evento.turnoId(), evento.canceladoEn());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void alReprogramar(TurnoReprogramado evento) {
        tokens.recalcularExpiracion(evento.turnoId(), evento.nuevo().inicio());
    }
}
