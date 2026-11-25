package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.application.AgendaLeida.BloqueoAgenda;
import ar.edu.utn.turnos.agenda.application.AgendaLeida.NombrePaciente;
import ar.edu.utn.turnos.agenda.application.AgendaLeida.TurnoAgenda;
import ar.edu.utn.turnos.agenda.domain.PeriodoAgenda;
import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios;
import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios.TramoSemanal;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.VistaAgenda;
import ar.edu.utn.turnos.agenda.infrastructure.AgendaLecturaPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoConsulta;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda diaria y semanal (C-28, US-013). La lectura es global para los tres roles; lo que cambia es qué
 * datos sensibles se completan: {@code ADMIN} y {@code RECEPCION} ven todo y {@code ODONTOLOGO} solo lo de su
 * profesional (RN-33). Lee de profesionales y pacientes únicamente por sus puertos de aplicación (KB 08).
 */
@Service
public class ConsultaAgenda {
    private final AgendaLecturaPersistencia turnos;
    private final HorarioConsulta horarios;
    private final BloqueoConsulta bloqueos;
    private final PacienteConsulta pacientes;
    private final Optional<ProfesionalVinculadoConsulta> vinculos;
    private final Clock reloj;

    public ConsultaAgenda(AgendaLecturaPersistencia turnos, HorarioConsulta horarios, BloqueoConsulta bloqueos,
            PacienteConsulta pacientes, Optional<ProfesionalVinculadoConsulta> vinculos, Clock reloj) {
        this.turnos = turnos;
        this.horarios = horarios;
        this.bloqueos = bloqueos;
        this.pacientes = pacientes;
        this.vinculos = vinculos;
        this.reloj = reloj;
    }

    /** {@code fecha} nula es hoy en Buenos Aires; los filtros nulos no filtran. */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')")
    @Transactional(readOnly = true)
    public AgendaLeida consultar(VistaAgenda vista, LocalDate fecha, Long profesionalId, Long sillonId) {
        Predicate<Long> puedeVerDetalle = quienConsulta();
        var periodo = PeriodoAgenda.de(vista, fecha != null ? fecha : LocalDate.now(reloj));
        var intervalo = periodo.intervalo();

        List<Turno> delPeriodo = turnos.turnosEnPeriodo(intervalo, profesionalId, sillonId);
        Map<Long, Paciente> nombres = pacientes.porIds(delPeriodo.stream()
                .filter(t -> puedeVerDetalle.test(t.profesionalId()))
                .map(Turno::pacienteId)
                .collect(Collectors.toSet()));
        var turnosAgenda = delPeriodo.stream()
                .map(t -> aTurnoAgenda(t, puedeVerDetalle.test(t.profesionalId()), nombres))
                .toList();

        var tramos = horarios.tramos(profesionalId).stream()
                .map(h -> new TramoSemanal(h.profesionalId(), h.diaSemana(), h.horaInicio(), h.horaFin()))
                .toList();
        var bloqueosAgenda = bloqueos.queSolapan(profesionalId, intervalo).stream()
                .map(b -> new BloqueoAgenda(b.id(), b.profesionalId(), b.inicio(), b.fin(),
                        puedeVerDetalle.test(b.profesionalId()) ? b.motivo() : null))
                .toList();

        return new AgendaLeida(periodo, turnosAgenda, ProyeccionHorarios.proyectar(tramos, periodo), bloqueosAgenda);
    }

    private static TurnoAgenda aTurnoAgenda(Turno t, boolean detalle, Map<Long, Paciente> nombres) {
        NombrePaciente paciente = detalle
                ? Optional.ofNullable(nombres.get(t.pacienteId()))
                        .map(p -> new NombrePaciente(p.nombre(), p.apellido())).orElse(null)
                : null;
        return new TurnoAgenda(t.id(), t.intervalo().inicio(), t.intervalo().fin(), t.duracionMin(),
                t.estado().name(), t.profesionalId(), t.sillonId(), t.prestacionId(), t.pacienteId(), paciente,
                detalle ? t.notas() : null);
    }

    /**
     * Qué profesionales puede ver en detalle quien consulta: todos para {@code ADMIN} y {@code RECEPCION};
     * solo el propio para {@code ODONTOLOGO}, que sin vínculo recibe 403.
     */
    private Predicate<Long> quienConsulta() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = autenticacion.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_RECEPCION")) {
            return profesional -> true;
        }
        Long propio = vinculos.flatMap(v -> v.buscarIdPorIdentidad(autenticacion.getName()))
                .orElseThrow(() -> new AccessDeniedException("El odontólogo no tiene un profesional vinculado"));
        return propio::equals;
    }
}
