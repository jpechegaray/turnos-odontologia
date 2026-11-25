package ar.edu.utn.turnos.shared.tiempo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Intervalo semiabierto {@code [inicio, fin)}: incluye el inicio y excluye el fin. Es el único lugar
 * donde vive la regla de bordes de la agenda (RN-02): un intervalo que empieza justo cuando termina
 * otro no se solapa con él.
 */
public record Intervalo(Instant inicio, Instant fin) {
    public Intervalo {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }

    /** Intervalo que empieza en {@code inicio} y dura {@code minutos} (RN-09: fin = inicio + duración). */
    public static Intervalo desde(Instant inicio, int minutos) {
        return new Intervalo(inicio, inicio.plus(Duration.ofMinutes(minutos)));
    }

    /** RN-01 y RN-02: comparten al menos un instante; tocarse en un borde no cuenta. */
    public boolean solapaCon(Intervalo otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    /** El otro intervalo cabe completo dentro de este (RN-16). */
    public boolean contiene(Intervalo otro) {
        return !otro.inicio.isBefore(inicio) && !otro.fin.isAfter(fin);
    }

    /**
     * Tramos de este intervalo que no comparten instantes con {@code otro}, en orden (C-35): vacío si lo cubre,
     * este completo si no se tocan y uno o dos tramos si se solapan en parte.
     */
    public List<Intervalo> menos(Intervalo otro) {
        if (!solapaCon(otro)) {
            return List.of(this);
        }
        var tramos = new ArrayList<Intervalo>(2);
        if (inicio.isBefore(otro.inicio)) {
            tramos.add(new Intervalo(inicio, otro.inicio));
        }
        if (otro.fin.isBefore(fin)) {
            tramos.add(new Intervalo(otro.fin, fin));
        }
        return List.copyOf(tramos);
    }
}
