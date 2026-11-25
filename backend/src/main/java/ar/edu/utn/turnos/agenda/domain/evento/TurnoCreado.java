package ar.edu.utn.turnos.agenda.domain.evento;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/**
 * Se creó un turno (Flujo 1). Lo publica la agenda al guardar; lo escucharán las notificaciones (C-15)
 * y el token del turno (C-32). Solo lleva ids y el intervalo: nunca datos personales.
 */
public record TurnoCreado(Long turnoId, Long pacienteId, Long profesionalId, Long sillonId, Intervalo intervalo) {

    public static TurnoCreado de(Turno turno) {
        if (turno.id() == null) {
            throw new IllegalArgumentException("El turno tiene que estar guardado para publicar TurnoCreado");
        }
        return new TurnoCreado(turno.id(), turno.pacienteId(), turno.profesionalId(), turno.sillonId(),
                turno.intervalo());
    }
}
