package ar.edu.utn.turnos.pacientes.web;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import java.time.Instant;

/**
 * DTO de salida de la ficha de paciente (KB 04 §Paciente). No expone {@code consentimiento_datos_en}:
 * lo setea la reserva pública (C-41) y no forma parte de la API de gestión.
 */
public record PacienteResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String email,
        String telefono,
        String obraSocial,
        String nroAfiliado,
        String notas,
        boolean activo,
        Instant creadoEn) {

    public static PacienteResponse desde(Paciente p) {
        return new PacienteResponse(p.id(), p.nombre(), p.apellido(), p.dni(), p.email(), p.telefono(),
                p.obraSocial(), p.nroAfiliado(), p.notas(), p.activo(), p.creadoEn());
    }
}
