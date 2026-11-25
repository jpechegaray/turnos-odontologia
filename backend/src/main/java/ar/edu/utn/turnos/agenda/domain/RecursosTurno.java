package ar.edu.utn.turnos.agenda.domain;

import java.util.Objects;

/**
 * Estado de los cuatro recursos de un turno, ya consultado por la capa de aplicación (RN-11).
 *
 * @param duracionDefaultMin           duración por defecto de la prestación; solo vale si la prestación existe
 * @param profesionalRealizaPrestacion si el profesional tiene asignada la prestación ({@code ProfesionalPrestacion})
 */
public record RecursosTurno(
        EstadoRecurso paciente,
        EstadoRecurso profesional,
        EstadoRecurso sillon,
        EstadoRecurso prestacion,
        int duracionDefaultMin,
        boolean profesionalRealizaPrestacion) {

    public RecursosTurno {
        Objects.requireNonNull(paciente, "paciente");
        Objects.requireNonNull(profesional, "profesional");
        Objects.requireNonNull(sillon, "sillon");
        Objects.requireNonNull(prestacion, "prestacion");
    }
}
