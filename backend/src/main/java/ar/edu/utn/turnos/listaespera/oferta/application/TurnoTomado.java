package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import java.time.Instant;

/**
 * Respuesta de la toma (C-42): el turno nuevo y su token en claro, que se muestra una única vez (RN-25). El token no
 * aparece en representaciones de texto.
 */
public record TurnoTomado(Instant inicio, Instant fin, EstadoTurno estado, String profesional, String prestacion,
        String token) {

    @Override
    public String toString() {
        return "TurnoTomado[inicio=" + inicio + ", fin=" + fin + ", estado=" + estado + ", token=***]";
    }
}
