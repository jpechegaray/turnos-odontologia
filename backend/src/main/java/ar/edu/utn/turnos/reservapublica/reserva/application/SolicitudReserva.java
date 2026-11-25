package ar.edu.utn.turnos.reservapublica.reserva.application;

import java.time.Instant;

/** Pedido de reserva online ya validado en su forma (RN-26: sin DNI). No imprime datos personales. */
public record SolicitudReserva(
        Long prestacionId,
        Long profesionalId,
        Instant inicio,
        String nombre,
        String apellido,
        String email,
        String telefono,
        String obraSocial,
        Boolean aceptaTratamientoDatos,
        String versionConsentimiento) {

    @Override
    public String toString() {
        return "SolicitudReserva[prestacionId=" + prestacionId + ", profesionalId=" + profesionalId + ", inicio="
                + inicio + ", datos personales ocultos]";
    }
}
