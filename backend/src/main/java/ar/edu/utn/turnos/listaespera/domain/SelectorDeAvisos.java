package ar.edu.utn.turnos.listaespera.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Elige a quién avisar entre las entradas compatibles con un hueco (RN-23, SU-14): por antigüedad, una sola
 * entrada por paciente (la más antigua) y hasta el tope. No avisa si el hueco ya empezó. Función pura.
 */
public final class SelectorDeAvisos {

    /** Antigüedad: {@code creadoEn} ascendente y, si empatan, {@code id} ascendente. */
    public static final Comparator<EntradaListaEspera> POR_ANTIGUEDAD = Comparator
            .comparing(EntradaListaEspera::creadoEn)
            .thenComparing(EntradaListaEspera::id, Comparator.nullsLast(Comparator.naturalOrder()));

    private SelectorDeAvisos() {
    }

    public static List<EntradaListaEspera> seleccionar(List<EntradaListaEspera> compatibles, Hueco hueco,
            Instant ahora, int tope) {
        if (tope <= 0 || !ahora.isBefore(hueco.inicio())) {
            return List.of();
        }
        var pacientes = new HashSet<Long>();
        // La deduplicación va antes del tope, así el tope cuenta pacientes distintos.
        return compatibles.stream()
                .sorted(POR_ANTIGUEDAD)
                .filter(entrada -> pacientes.add(entrada.pacienteId()))
                .limit(tope)
                .toList();
    }
}
