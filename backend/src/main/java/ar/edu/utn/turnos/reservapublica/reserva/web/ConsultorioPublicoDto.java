package ar.edu.utn.turnos.reservapublica.reserva.web;

/**
 * Datos públicos del consultorio para el aviso de consentimiento (PA-08): solo estos cinco, nunca el resto de
 * la configuración.
 */
public record ConsultorioPublicoDto(String nombre, String domicilio, String emailContacto, int retencionAnios,
        String versionConsentimiento) {
}
