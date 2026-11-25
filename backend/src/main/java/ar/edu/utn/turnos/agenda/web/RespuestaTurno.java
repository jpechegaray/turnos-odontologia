package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.domain.Turno;
import java.time.Instant;

/** Representación de un turno en la API: nunca la entidad (regla dura 9). Los instantes van en UTC. */
record RespuestaTurno(
        Long id,
        Long pacienteId,
        Long profesionalId,
        Long sillonId,
        Long prestacionId,
        Instant inicio,
        Instant fin,
        int duracionMin,
        String estado,
        String canal,
        String notas,
        Instant creadoEn) {

    static RespuestaTurno de(Turno turno) {
        return new RespuestaTurno(turno.id(), turno.pacienteId(), turno.profesionalId(), turno.sillonId(),
                turno.prestacionId(), turno.intervalo().inicio(), turno.intervalo().fin(), turno.duracionMin(),
                turno.estado().name(), turno.canal().name(), turno.notas(), turno.creadoEn());
    }
}
