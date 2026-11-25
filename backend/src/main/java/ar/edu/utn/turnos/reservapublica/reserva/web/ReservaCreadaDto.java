package ar.edu.utn.turnos.reservapublica.reserva.web;

import ar.edu.utn.turnos.reservapublica.reserva.application.ReservaCreada;
import java.time.Instant;

/** Respuesta 201: igual exista o no la ficha, sin ids de paciente, sillón ni turno (RN-35). */
public record ReservaCreadaDto(Instant inicio, Instant fin, String prestacion, String profesional, String token) {

    static ReservaCreadaDto de(ReservaCreada reserva) {
        return new ReservaCreadaDto(reserva.inicio(), reserva.fin(), reserva.prestacion(), reserva.profesional(),
                reserva.token());
    }

    @Override
    public String toString() {
        return "ReservaCreadaDto[inicio=" + inicio + ", fin=" + fin + ", token=***]";
    }
}
