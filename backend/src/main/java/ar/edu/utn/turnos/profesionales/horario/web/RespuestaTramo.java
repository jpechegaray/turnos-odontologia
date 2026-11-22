package ar.edu.utn.turnos.profesionales.horario.web;

import ar.edu.utn.turnos.profesionales.horario.application.HorarioDatos;
import java.time.format.DateTimeFormatter;

/** Representación de un tramo con las horas en {@code HH:mm}, igual que en la solicitud. */
record RespuestaTramo(Long id, Long profesionalId, int diaSemana, String horaInicio, String horaFin) {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    static RespuestaTramo de(HorarioDatos datos) {
        return new RespuestaTramo(datos.id(), datos.profesionalId(), datos.diaSemana(),
                datos.horaInicio().format(HH_MM), datos.horaFin().format(HH_MM));
    }
}
