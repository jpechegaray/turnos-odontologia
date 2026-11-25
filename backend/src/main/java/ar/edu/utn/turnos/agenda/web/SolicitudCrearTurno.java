package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Cuerpo de {@code POST /api/turnos}. {@code inicio} es un instante ISO-8601 con zona (por ejemplo
 * {@code 2030-06-10T10:00:00-03:00}); una fecha sin zona no se acepta (DD-05). La duración es opcional (RN-03).
 */
record SolicitudCrearTurno(
        @NotNull Long pacienteId,
        @NotNull Long profesionalId,
        @NotNull Long sillonId,
        @NotNull Long prestacionId,
        @NotNull OffsetDateTime inicio,
        Integer duracionMin,
        @Size(max = 1000) String notas) {

    SolicitudTurno dominio() {
        return new SolicitudTurno(pacienteId, profesionalId, sillonId, prestacionId, inicio.toInstant(), duracionMin);
    }
}
