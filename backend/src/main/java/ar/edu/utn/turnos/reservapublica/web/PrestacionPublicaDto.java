package ar.edu.utn.turnos.reservapublica.web;

import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;

/** Prestación reservable tal como la ve el paciente anónimo. */
public record PrestacionPublicaDto(Long id, String nombre, int duracionMin) {

    static PrestacionPublicaDto de(PrestacionResumen prestacion) {
        return new PrestacionPublicaDto(prestacion.id(), prestacion.nombre(), prestacion.duracionDefaultMin());
    }
}
