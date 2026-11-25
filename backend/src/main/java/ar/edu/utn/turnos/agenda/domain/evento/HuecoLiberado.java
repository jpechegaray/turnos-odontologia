package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;

/**
 * Quedó libre un horario de un profesional por una cancelación o una reprogramación (RN-06, RN-18). Lo
 * publicarán C-27 y C-35; lo escuchará la oferta de huecos de la lista de espera (C-36, RN-23).
 */
public record HuecoLiberado(Long profesionalId, Long sillonId, Intervalo hueco, Long turnoOrigenId) {

    /** Hueco que deja un turno cancelado (RN-06). */
    public static HuecoLiberado de(Turno cancelado) {
        TurnoCancelado.exigirGuardadoYCancelado(cancelado);
        return new HuecoLiberado(cancelado.profesionalId(), cancelado.sillonId(), cancelado.intervalo(),
                cancelado.id());
    }

    /**
     * Huecos que deja una reprogramación (RN-18, C-35): con el mismo profesional, los tramos del intervalo anterior
     * que el nuevo ya no ocupa; con otro profesional, el intervalo anterior completo. Siempre con el profesional y el
     * sillón anteriores; el sillón es indistinto para la lista de espera (SU-14 c).
     */
    public static List<HuecoLiberado> porReprogramacion(Turno anterior, Turno nuevo) {
        var tramos = anterior.profesionalId().equals(nuevo.profesionalId())
                ? anterior.intervalo().menos(nuevo.intervalo())
                : List.of(anterior.intervalo());
        return tramos.stream()
                .map(tramo -> new HuecoLiberado(anterior.profesionalId(), anterior.sillonId(), tramo, anterior.id()))
                .toList();
    }
}
