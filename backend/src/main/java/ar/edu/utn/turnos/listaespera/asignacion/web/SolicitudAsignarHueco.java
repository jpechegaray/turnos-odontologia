package ar.edu.utn.turnos.listaespera.asignacion.web;

import ar.edu.utn.turnos.listaespera.asignacion.application.SolicitudAsignacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Cuerpo de {@code POST /api/lista-espera/{id}/asignar} (C-43, design D8). {@code inicio} es un instante
 * ISO-8601 con zona; una fecha sin zona no se acepta (DD-05). {@code sillonId} y {@code notas} son opcionales.
 */
public record SolicitudAsignarHueco(
        @NotNull Long profesionalId,
        @NotNull OffsetDateTime inicio,
        Long sillonId,
        @Size(max = 1000) String notas) {

    public SolicitudAsignacion dominio() {
        return new SolicitudAsignacion(profesionalId, inicio.toInstant(), sillonId, notas);
    }
}
