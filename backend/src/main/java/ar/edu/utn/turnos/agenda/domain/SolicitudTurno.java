package ar.edu.utn.turnos.agenda.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Lo que pide quien da el turno. {@code duracionMin} es opcional: si falta, se usa la de la prestación (RN-03).
 */
public record SolicitudTurno(
        Long pacienteId,
        Long profesionalId,
        Long sillonId,
        Long prestacionId,
        Instant inicio,
        Integer duracionMin) {

    public SolicitudTurno {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
