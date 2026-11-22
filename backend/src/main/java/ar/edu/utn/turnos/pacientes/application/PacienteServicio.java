package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import java.util.List;

/**
 * Port for patient operations (writes and queries).
 */
public interface PacienteServicio {

    /**
     * Crea una nueva ficha de paciente.
     */
    Paciente crear(Paciente paciente);

    /**
     * Actualiza una ficha existente.
     */
    Paciente actualizar(Long id, Paciente paciente);

    /**
     * Obtiene una ficha por su identificador.
     */
    Paciente obtener(Long id);

    /**
     * Busca pacientes con filtros opcionales.
     */
    List<Paciente> buscar(String apellido, String nombre, String dni, String email);

    /**
     * Da de baja lógicamente una ficha.
     */
    void darDeBaja(Long id);
}
