package ar.edu.utn.turnos.reservapublica.reserva.application;

import java.time.Instant;

/** Resultado de la reserva: el turno sin ids ni datos de la ficha, y el token en claro (una sola vez, RN-25). */
public record ReservaCreada(Instant inicio, Instant fin, String prestacion, String profesional, String token) {

    @Override
    public String toString() {
        return "ReservaCreada[inicio=" + inicio + ", fin=" + fin + ", token=***]";
    }
}
