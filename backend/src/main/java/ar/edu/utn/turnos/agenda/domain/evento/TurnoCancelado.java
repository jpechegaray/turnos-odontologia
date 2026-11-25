package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;

/** Se canceló un turno (RN-06). Lo publicará C-27; lo escucharán notificaciones y token (revocación). */
public record TurnoCancelado(Long turnoId, Long pacienteId, Long profesionalId, Long sillonId, Intervalo intervalo,
        boolean cancelacionTardia, Instant canceladoEn) {
}
