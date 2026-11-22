package ar.edu.utn.turnos.profesionales.profesional.application;

import java.util.List;
import java.util.Optional;

public interface ProfesionalConsulta {

    List<Datos> listarActivos();

    Optional<Datos> buscarPorId(Long id);

    record Datos(Long id, String nombre, String apellido, String matricula, String especialidad, boolean activo) {
    }
}
