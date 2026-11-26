package ar.edu.utn.turnos.listaespera.asignacion.application;

import java.time.Instant;
import java.util.Objects;

/**
 * Lo que recepción envía para asignar un hueco a una entrada de la lista de espera (C-43). Solo el inicio del
 * hueco: la duración sale de la prestación de la entrada (design D2). El sillón es opcional (design D4).
 */
public record SolicitudAsignacion(Long profesionalId, Instant inicio, Long sillonId, String notas) {

    public SolicitudAsignacion {
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(inicio, "inicio");
    }
}
