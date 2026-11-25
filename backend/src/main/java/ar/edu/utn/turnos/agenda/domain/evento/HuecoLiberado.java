package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/**
 * Quedó libre un horario de un profesional por una cancelación o una reprogramación (RN-06, RN-18). Lo
 * publicarán C-27 y C-35; lo escuchará la oferta de huecos de la lista de espera (C-36, RN-23).
 */
public record HuecoLiberado(Long profesionalId, Long sillonId, Intervalo hueco, Long turnoOrigenId) {
}
