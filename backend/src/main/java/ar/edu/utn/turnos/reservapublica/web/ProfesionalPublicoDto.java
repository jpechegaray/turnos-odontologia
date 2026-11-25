package ar.edu.utn.turnos.reservapublica.web;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;

/** Profesional reservable sin datos internos (no incluye la matrícula). */
public record ProfesionalPublicoDto(Long id, String nombre, String apellido, String especialidad) {

    static ProfesionalPublicoDto de(ProfesionalConsulta.Datos profesional) {
        return new ProfesionalPublicoDto(profesional.id(), profesional.nombre(), profesional.apellido(),
                profesional.especialidad());
    }
}
