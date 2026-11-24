package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import java.util.Optional;

/**
 * Port for read‑only access to patient data used by other modules (e.g., agenda, reserva pública).
 */
public interface PacienteConsulta {

    /**
     * Busca una ficha de paciente por su identificador.
     * Devuelve {@link Optional#empty()} si no existe.
     */
    Optional<Paciente> porId(Long id);

    /**
     * Busca una ficha de paciente por su email (ignora mayúsculas/minúsculas).
     * Devuelve {@link Optional#empty()} si no existe.
     */
    Optional<Paciente> porEmail(String email);
}
