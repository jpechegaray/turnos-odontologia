package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Fichas de los identificadores pedidos, indexadas por id, en una sola consulta (C-28: nombres en la
     * agenda sin N+1). Los ids inexistentes se ignoran; un conjunto vacío devuelve un mapa vacío.
     */
    Map<Long, Paciente> porIds(Set<Long> ids);
}
