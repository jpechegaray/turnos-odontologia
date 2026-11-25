package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.EstadoRecurso;
import ar.edu.utn.turnos.agenda.domain.RecursosTurno;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Carga, con los puertos de los otros módulos, el estado de los recursos que {@code ValidadorTurno} revisa (RN-11).
 * La comparten el alta (C-19) y la reprogramación (C-35).
 */
@Component
class ConsultaRecursosTurno {
    private final PacienteConsulta pacientes;
    private final ProfesionalConsulta profesionales;
    private final SillonConsulta sillones;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion profesionalPrestaciones;

    ConsultaRecursosTurno(PacienteConsulta pacientes, ProfesionalConsulta profesionales, SillonConsulta sillones,
            ConsultaPrestaciones prestaciones, ProfesionalRealizaPrestacion profesionalPrestaciones) {
        this.pacientes = pacientes;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
        this.profesionalPrestaciones = profesionalPrestaciones;
    }

    RecursosTurno de(SolicitudTurno solicitud) {
        Optional<PrestacionResumen> prestacion = prestaciones.buscarPorId(solicitud.prestacionId());
        return new RecursosTurno(
                estado(pacientes.porId(solicitud.pacienteId()).map(p -> p.activo())),
                estado(profesionales.buscarPorId(solicitud.profesionalId()).map(p -> p.activo())),
                estado(sillones.buscarPorId(solicitud.sillonId()).map(s -> s.activo())),
                estado(prestacion.map(PrestacionResumen::activa)),
                prestacion.map(PrestacionResumen::duracionDefaultMin).orElse(0),
                profesionalPrestaciones.realiza(solicitud.profesionalId(), solicitud.prestacionId()));
    }

    private static EstadoRecurso estado(Optional<Boolean> activo) {
        return activo.map(a -> a ? EstadoRecurso.ACTIVO : EstadoRecurso.INACTIVO).orElse(EstadoRecurso.INEXISTENTE);
    }
}
