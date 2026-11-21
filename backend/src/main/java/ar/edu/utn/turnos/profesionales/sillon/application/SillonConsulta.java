package ar.edu.utn.turnos.profesionales.sillon.application;

import java.util.List;
import java.util.Optional;

/** Consultas internas de agenda: «activo» no implica que el sillón esté libre. */
public interface SillonConsulta {

    List<Datos> listarActivos();

    Optional<Datos> buscarPorId(Long id);

    record Datos(Long id, String nombre, String descripcion, boolean activo) {
    }
}
