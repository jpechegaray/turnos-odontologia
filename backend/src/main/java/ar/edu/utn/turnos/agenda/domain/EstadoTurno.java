package ar.edu.utn.turnos.agenda.domain;

import java.util.Map;
import java.util.Set;

/**
 * Estados del turno y su tabla de transiciones (RN-12). Nace {@code PROGRAMADO}; {@code CANCELADO},
 * {@code ATENDIDO} y {@code AUSENTE} son finales. Solo {@code CANCELADO} libera el lugar (RN-10).
 */
public enum EstadoTurno {
    PROGRAMADO,
    CONFIRMADO,
    ATENDIDO,
    AUSENTE,
    CANCELADO;

    private static final Map<EstadoTurno, Set<EstadoTurno>> TRANSICIONES = Map.of(
            PROGRAMADO, Set.of(CONFIRMADO, CANCELADO, ATENDIDO, AUSENTE),
            CONFIRMADO, Set.of(CANCELADO, ATENDIDO, AUSENTE),
            ATENDIDO, Set.of(),
            AUSENTE, Set.of(),
            CANCELADO, Set.of());

    public boolean puedePasarA(EstadoTurno destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }

    /** RN-10: los cancelados no ocupan lugar; atendidos y ausentes sí (su horario ya transcurrió). */
    public boolean ocupaLugar() {
        return this != CANCELADO;
    }
}
