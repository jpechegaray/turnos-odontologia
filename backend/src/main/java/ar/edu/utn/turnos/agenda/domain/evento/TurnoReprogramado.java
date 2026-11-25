package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/**
 * Se movió un turno conservando su identidad (RN-18). Lo publica C-35; lleva el intervalo anterior y el
 * nuevo, y los recursos anteriores y nuevos, para que el token recalcule su vencimiento (RN-25).
 */
public record TurnoReprogramado(Long turnoId, Long pacienteId,
        Long profesionalAnteriorId, Long sillonAnteriorId, Intervalo anterior,
        Long profesionalNuevoId, Long sillonNuevoId, Intervalo nuevo,
        boolean reprogramadoTarde) {

    public static TurnoReprogramado de(Turno anterior, Turno nuevo) {
        return new TurnoReprogramado(anterior.id(), anterior.pacienteId(), anterior.profesionalId(),
                anterior.sillonId(), anterior.intervalo(), nuevo.profesionalId(), nuevo.sillonId(), nuevo.intervalo(),
                nuevo.reprogramadoTarde());
    }
}
