package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/** Un turno existente que podría chocar con el que se valida (RN-01). */
public record TurnoOcupado(Long turnoId, Long profesionalId, Long sillonId, Intervalo intervalo, EstadoTurno estado) {
}
