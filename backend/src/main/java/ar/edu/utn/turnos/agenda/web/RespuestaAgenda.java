package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.AgendaLeida;
import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios.HorarioDelDia;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Agenda en la API (C-28). Los instantes salen en hora de Buenos Aires con su desfase (RN-14), por ejemplo
 * {@code 2026-12-07T23:30:00-03:00}: siguen siendo inequívocos y se leen en hora local.
 */
record RespuestaAgenda(String vista, LocalDate desde, LocalDate hasta, List<Turno> turnos, List<Horario> horarios,
        List<Bloqueo> bloqueos) {

    record Paciente(String nombre, String apellido) {
    }

    record Turno(Long id, OffsetDateTime inicio, OffsetDateTime fin, int duracionMin, String estado,
            Long profesionalId, Long sillonId, Long prestacionId, Long pacienteId, Paciente paciente, String notas) {
    }

    record Horario(Long profesionalId, LocalDate fecha, OffsetDateTime inicio, OffsetDateTime fin) {
    }

    record Bloqueo(Long id, Long profesionalId, OffsetDateTime inicio, OffsetDateTime fin, String motivo) {
    }

    static RespuestaAgenda de(AgendaLeida agenda) {
        var periodo = agenda.periodo();
        return new RespuestaAgenda(periodo.vista().name().toLowerCase(Locale.ROOT), periodo.desde(), periodo.hasta(),
                agenda.turnos().stream().map(RespuestaAgenda::turno).toList(),
                agenda.horarios().stream().map(RespuestaAgenda::horario).toList(),
                agenda.bloqueos().stream().map(b -> new Bloqueo(b.id(), b.profesionalId(), local(b.inicio()),
                        local(b.fin()), b.motivo())).toList());
    }

    private static Turno turno(AgendaLeida.TurnoAgenda t) {
        Paciente paciente = t.paciente() == null ? null : new Paciente(t.paciente().nombre(), t.paciente().apellido());
        return new Turno(t.id(), local(t.inicio()), local(t.fin()), t.duracionMin(), t.estado(), t.profesionalId(),
                t.sillonId(), t.prestacionId(), t.pacienteId(), paciente, t.notas());
    }

    private static Horario horario(HorarioDelDia h) {
        return new Horario(h.profesionalId(), h.fecha(), local(h.inicio()), local(h.fin()));
    }

    private static OffsetDateTime local(Instant instante) {
        return instante.atZone(ZonaHoraria.BUENOS_AIRES).toOffsetDateTime();
    }
}
