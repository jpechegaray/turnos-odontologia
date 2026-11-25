package ar.edu.utn.turnos.listaespera.oferta.application;

import java.time.Instant;

/**
 * Lo que ve el paciente al abrir el enlace de la oferta (C-42, Flujo 7 paso 1): el turno que se crearía y el estado.
 * Sin ids ni datos de pacientes (RN-07, RN-38).
 *
 * @param fin inicio del hueco más la duración por defecto de la prestación de la entrada
 */
public record OfertaPublica(Instant inicio, Instant fin, String profesional, String prestacion, Estado estado) {

    /** Estado de la oferta tal como lo informa el enlace. */
    public enum Estado {
        DISPONIBLE,
        TOMADA,
        VENCIDA,
        NO_DISPONIBLE
    }
}
