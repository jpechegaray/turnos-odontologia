package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.PeriodoAgenda;
import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios.HorarioDelDia;
import java.time.Instant;
import java.util.List;

/**
 * Modelo de lectura de la agenda (C-28), con los datos sensibles ya filtrados según quién consulta:
 * {@code paciente}, {@code notas} y {@code motivo} llegan nulos cuando el rol no puede verlos.
 */
public record AgendaLeida(PeriodoAgenda periodo, List<TurnoAgenda> turnos, List<HorarioDelDia> horarios,
        List<BloqueoAgenda> bloqueos) {

    public record NombrePaciente(String nombre, String apellido) {
    }

    public record TurnoAgenda(Long id, Instant inicio, Instant fin, int duracionMin, String estado,
            Long profesionalId, Long sillonId, Long prestacionId, Long pacienteId, NombrePaciente paciente,
            String notas) {
    }

    public record BloqueoAgenda(Long id, Long profesionalId, Instant inicio, Instant fin, String motivo) {
    }
}
