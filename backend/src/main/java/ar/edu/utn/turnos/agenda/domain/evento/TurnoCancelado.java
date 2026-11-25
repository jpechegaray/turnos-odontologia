package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;

/** Se canceló un turno (RN-06). Lo publicará C-27; lo escucharán notificaciones y token (revocación). */
public record TurnoCancelado(Long turnoId, Long pacienteId, Long profesionalId, Long sillonId, Intervalo intervalo,
        boolean cancelacionTardia, Instant canceladoEn) {

    /** El turno tiene que estar guardado y cancelado (C-27). */
    public static TurnoCancelado de(Turno turno) {
        exigirGuardadoYCancelado(turno);
        return new TurnoCancelado(turno.id(), turno.pacienteId(), turno.profesionalId(), turno.sillonId(),
                turno.intervalo(), turno.cancelacionTardia(), turno.canceladoEn());
    }

    static void exigirGuardadoYCancelado(Turno turno) {
        if (turno.id() == null || turno.estado() != EstadoTurno.CANCELADO) {
            throw new IllegalArgumentException("El evento exige un turno guardado y cancelado");
        }
    }
}
