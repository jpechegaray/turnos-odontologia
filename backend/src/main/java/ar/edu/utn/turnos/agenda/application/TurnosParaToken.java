package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import java.time.Instant;
import java.util.Optional;

public interface TurnosParaToken {
    /** Lee el turno y lo bloquea para escritura hasta que termine la transacción del llamador. */
    Optional<EstadoActual> bloquearParaToken(Long turnoId);

    record EstadoActual(Instant inicio, EstadoTurno estado) {
    }
}
