package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.ReprogramarTurnoService;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Cuerpo de {@code POST /api/turnos/{id}/reprogramar}: solo el inicio es obligatorio; no cambia las notas. */
record SolicitudReprogramarTurno(
        @NotNull OffsetDateTime inicio,
        Long profesionalId,
        Long sillonId,
        Integer duracionMin) {

    ReprogramarTurnoService.Pedido pedido() {
        return new ReprogramarTurnoService.Pedido(inicio.toInstant(), profesionalId, sillonId, duracionMin);
    }
}
