package ar.edu.utn.turnos.agenda.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * RN-05: una cancelación es tardía cuando falta menos que el plazo configurado para el inicio del turno.
 * El límite es inclusivo: a plazo exacto no es tardía. Se compara {@link Duration} contra {@link Duration},
 * sin redondear a horas, para que el borde de 23:59 sea exacto. La marca se calcula al cancelar y queda
 * guardada: un cambio posterior del plazo no altera cancelaciones anteriores.
 */
public final class PoliticaCancelacion {

    private PoliticaCancelacion() {
    }

    /** {@code tardía = (inicio - ahora) < plazo}. */
    public static boolean esTardia(Instant inicio, Instant ahora, Duration plazo) {
        return Duration.between(ahora, inicio).compareTo(plazo) < 0;
    }
}
