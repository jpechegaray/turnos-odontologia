package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;

/**
 * Puerto para que la reserva online (C-41, US-015) cree la ficha de un paciente nuevo sin la autorización del
 * personal. Solo crea: nunca modifica una ficha existente (RN-28). Se llama dentro de la transacción de la reserva.
 */
public interface AltaPacienteReservaOnline {

    /** Datos que el paciente carga en la reserva (RN-26): sin DNI. */
    record DatosFicha(String nombre, String apellido, String email, String telefono, String obraSocial) {

        @Override
        public String toString() {
            return "DatosFicha[datos personales ocultos]";
        }
    }

    /** Crea la ficha activa con el consentimiento aceptado ahora y la versión del texto (PA-08). */
    Paciente registrar(DatosFicha datos, String versionConsentimiento);
}
