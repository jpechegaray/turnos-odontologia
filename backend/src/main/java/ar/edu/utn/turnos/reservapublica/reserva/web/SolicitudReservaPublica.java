package ar.edu.utn.turnos.reservapublica.reserva.web;

import ar.edu.utn.turnos.reservapublica.reserva.application.SolicitudReserva;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Cuerpo de {@code POST /api/public/reservas} (RN-26): sin DNI. La aceptación y la versión del consentimiento
 * no llevan Bean Validation: se validan como consentimiento (422) y no como forma del pedido (400).
 */
public record SolicitudReservaPublica(
        @NotNull Long prestacionId,
        @NotNull Long profesionalId,
        @NotNull OffsetDateTime inicio,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 100) String obraSocial,
        Boolean aceptaTratamientoDatos,
        String versionConsentimiento) {

    /** Los textos se recortan antes de validarlos: un email con espacios en los extremos es el mismo (RN-28). */
    public SolicitudReservaPublica {
        nombre = recortar(nombre);
        apellido = recortar(apellido);
        email = recortar(email);
        telefono = recortar(telefono);
        obraSocial = recortar(obraSocial);
    }

    SolicitudReserva aSolicitud() {
        return new SolicitudReserva(prestacionId, profesionalId, inicio.toInstant(), nombre, apellido, email,
                telefono, obraSocial, aceptaTratamientoDatos, versionConsentimiento);
    }

    private static String recortar(String texto) {
        return texto == null ? null : texto.strip();
    }

    @Override
    public String toString() {
        return "SolicitudReservaPublica[prestacionId=" + prestacionId + ", profesionalId=" + profesionalId
                + ", inicio=" + inicio + ", datos personales ocultos]";
    }
}
