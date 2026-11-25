package ar.edu.utn.turnos.listaespera.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Hueco libre de un profesional, {@code [inicio, fin)}. El sillón es indistinto (RN-11, SU-14 c).
 * Es propio de la lista de espera para no depender del dominio de agenda.
 */
public record Hueco(Long profesionalId, Instant inicio, Instant fin) {

    public Hueco {
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin del hueco debe ser posterior al inicio");
        }
    }

    public Duration duracion() {
        return Duration.between(inicio, fin);
    }
}
