package ar.edu.utn.turnos.shared.tiempo;

import java.time.Instant;

/** Intervalo semiabierto: incluye el inicio y excluye el fin. */
public record Intervalo(Instant inicio, Instant fin) {
    public Intervalo {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }
}
