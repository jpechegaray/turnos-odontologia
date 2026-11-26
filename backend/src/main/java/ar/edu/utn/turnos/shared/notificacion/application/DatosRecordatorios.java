package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.application.TurnosParaRecordatorio;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class DatosRecordatorios implements ReconstructorDatosNotificacion {
    private final TurnosParaRecordatorio turnos;
    private final PacienteConsulta pacientes;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final ServicioTokenTurno tokens;
    private final Clock reloj;
    private final String urlPublica;

    DatosRecordatorios(TurnosParaRecordatorio turnos, PacienteConsulta pacientes, ConsultaPrestaciones prestaciones,
            ProfesionalConsulta profesionales, ServicioTokenTurno tokens, Clock reloj,
            @Value("${APP_PUBLIC_URL:http://localhost:5173}") String urlPublica) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.tokens = tokens;
        this.reloj = reloj;
        this.urlPublica = urlPublica.strip().replaceFirst("/+$", "");
    }

    @Override
    public boolean descartarSiFaltanDatos(NotificacionEnviada notificacion) {
        return notificacion.tipo() == TipoNotificacion.RECORDATORIO;
    }

    @Override
    public Optional<DatosNotificacion> datosPara(NotificacionEnviada aviso) {
        if (!descartarSiFaltanDatos(aviso) || aviso.turnoId() == null) {
            return Optional.empty();
        }
        var turno = turnos.bloquear(aviso.turnoId()).orElse(null);
        if (turno == null || (turno.estado() != EstadoTurno.PROGRAMADO && turno.estado() != EstadoTurno.CONFIRMADO)
                || !turno.intervalo().inicio().equals(aviso.inicioReferencia())
                || !turno.intervalo().inicio().isAfter(reloj.instant())) {
            return Optional.empty();
        }
        var paciente = pacientes.porId(turno.pacienteId()).orElse(null);
        if (paciente == null || paciente.email() == null || paciente.email().isBlank()) {
            return Optional.empty();
        }
        var prestacion = prestaciones.buscarPorId(turno.prestacionId()).map(PrestacionResumen::nombre).orElse(null);
        var profesional = profesionales.buscarPorId(turno.profesionalId())
                .map(p -> p.nombre() + " " + p.apellido()).orElse(null);
        return tokens.emitirSiVigente(turno.id()).map(token -> new DatosNotificacion(paciente.email(),
                paciente.nombre(), turno.intervalo().inicio(), prestacion, profesional,
                urlPublica + "/turno/" + token.claro()));
    }
}
