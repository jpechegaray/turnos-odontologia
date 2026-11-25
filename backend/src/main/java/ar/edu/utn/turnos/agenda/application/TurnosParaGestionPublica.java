package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Operaciones de agenda para el turno ya identificado y bloqueado por el validador del enlace. */
@Service
public class TurnosParaGestionPublica {
    private final TurnosPersistencia turnos;
    private final ProfesionalConsulta profesionales;
    private final ConsultaPrestaciones prestaciones;

    TurnosParaGestionPublica(TurnosPersistencia turnos, ProfesionalConsulta profesionales,
            ConsultaPrestaciones prestaciones) {
        this.turnos = turnos;
        this.profesionales = profesionales;
        this.prestaciones = prestaciones;
    }

    public Datos consultar(Long id) {
        return proyectar(turnos.buscar(id).orElseThrow());
    }

    public Optional<Datos> confirmar(Long id, Instant ahora) {
        return turnos.buscar(id)
                .filter(turno -> ahora.isBefore(turno.intervalo().inicio()))
                .map(turno -> proyectar(turnos.guardar(turno.pasarA(EstadoTurno.CONFIRMADO))));
    }

    private Datos proyectar(Turno turno) {
        var profesional = profesionales.buscarPorId(turno.profesionalId()).orElseThrow();
        var prestacion = prestaciones.buscarPorId(turno.prestacionId()).orElseThrow();
        return new Datos(turno.intervalo().inicio(), turno.intervalo().fin(), turno.estado(),
                profesional.nombre() + " " + profesional.apellido(), prestacion.nombre());
    }

    public record Datos(Instant inicio, Instant fin, EstadoTurno estado, String profesional, String prestacion) {
    }
}
