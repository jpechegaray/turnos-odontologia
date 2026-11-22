package ar.edu.utn.turnos.prestaciones.web;

import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;

/** Contrato de salida de {@code /api/prestaciones}, desacoplado de la entidad JPA. */
record PrestacionRespuesta(Long id, String nombre, int duracionDefaultMin, boolean activa) {

    static PrestacionRespuesta de(PrestacionResumen resumen) {
        return new PrestacionRespuesta(resumen.id(), resumen.nombre(), resumen.duracionDefaultMin(),
                resumen.activa());
    }
}
