package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/**
 * Se movió un turno conservando su identidad (RN-18). Lo publicará C-35; lleva el intervalo anterior y el
 * nuevo, y los recursos anteriores y nuevos, para que el token recalcule su vencimiento (RN-25).
 */
public record TurnoReprogramado(Long turnoId, Long pacienteId,
        Long profesionalAnteriorId, Long sillonAnteriorId, Intervalo anterior,
        Long profesionalNuevoId, Long sillonNuevoId, Intervalo nuevo,
        boolean reprogramadoTarde) {
}
