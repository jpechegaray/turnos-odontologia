package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class DatosAvisosAgendamiento {
    private final PacienteConsulta pacientes;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;

    DatosAvisosAgendamiento(PacienteConsulta pacientes, ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales) {
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
    }

    Optional<DatosNotificacion> paraTurno(Turno turno, Instant inicio) {
        return paraHorario(turno.pacienteId(), turno.prestacionId(), turno.profesionalId(), inicio);
    }

    Optional<DatosNotificacion> paraEntrada(EntradaListaEspera entrada) {
        return paraHorario(entrada.pacienteId(), entrada.prestacionId(), null, null);
    }

    Optional<DatosNotificacion> paraHorario(Long pacienteId, Long prestacionId, Long profesionalId, Instant inicio) {
        return pacientes.porId(pacienteId)
                .filter(paciente -> paciente.activo() && paciente.email() != null && !paciente.email().isBlank())
                .map(paciente -> new DatosNotificacion(paciente.email(), paciente.nombre(), inicio,
                        prestaciones.buscarPorId(prestacionId).map(PrestacionResumen::nombre).orElse(null),
                        profesionalId == null ? null : profesionales.buscarPorId(profesionalId)
                                .map(p -> p.nombre() + " " + p.apellido()).orElse(null), null));
    }
}
