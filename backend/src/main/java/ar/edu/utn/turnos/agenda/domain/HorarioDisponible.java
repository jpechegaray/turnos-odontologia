package ar.edu.utn.turnos.agenda.domain;

import java.time.Instant;

/** Una sugerencia de agenda; debe revalidarse al crear el turno. */
public record HorarioDisponible(Instant inicio, Instant fin, Long sillonId) {
}
