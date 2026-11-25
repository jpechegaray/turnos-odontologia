package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.EstadoRecurso;
import ar.edu.utn.turnos.agenda.domain.RecursosTurno;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCreado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea un turno dado por el personal (US-010, Flujo 1): carga los recursos con los puertos de los otros
 * módulos, aplica las reglas puras de {@link ValidadorTurno}, guarda y publica {@link TurnoCreado}. Si otra
 * transacción ocupó el hueco mientras tanto, la persistencia responde el mismo 409 (RN-15).
 */
@Service
public class CrearTurnoService {
    private final TurnosPersistencia turnos;
    private final PacienteConsulta pacientes;
    private final ProfesionalConsulta profesionales;
    private final SillonConsulta sillones;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion profesionalPrestaciones;
    private final ConsultasAgenda consultas;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final ValidadorTurno validador;

    public CrearTurnoService(TurnosPersistencia turnos, PacienteConsulta pacientes, ProfesionalConsulta profesionales,
            SillonConsulta sillones, ConsultaPrestaciones prestaciones,
            ProfesionalRealizaPrestacion profesionalPrestaciones, ConsultasAgenda consultas,
            ApplicationEventPublisher eventos, Clock reloj) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
        this.profesionalPrestaciones = profesionalPrestaciones;
        this.consultas = consultas;
        this.eventos = eventos;
        this.reloj = reloj;
        this.validador = new ValidadorTurno(reloj);
    }

    /**
     * @param creadoPorUsuarioId usuario del personal que da el turno (trazabilidad); puede ser null
     */
    @Transactional
    public Turno crear(SolicitudTurno solicitud, String notas, Long creadoPorUsuarioId) {
        var intervalo = validador.validar(solicitud, recursos(solicitud), consultas, null);
        var turno = turnos.guardar(Turno.nuevo(solicitud.pacienteId(), solicitud.profesionalId(), solicitud.sillonId(),
                solicitud.prestacionId(), intervalo, notas, creadoPorUsuarioId, Instant.now(reloj)));
        eventos.publishEvent(TurnoCreado.de(turno));
        return turno;
    }

    private RecursosTurno recursos(SolicitudTurno solicitud) {
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
