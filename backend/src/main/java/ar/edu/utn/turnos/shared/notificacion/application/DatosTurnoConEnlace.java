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

/**
 * Rearma el email de un turno vigente con un enlace {@code /turno/<token>} y un token nuevo (RN-07: la fila nunca
 * guarda el token). Lo comparten el recordatorio (C-37) y la confirmación de reserva (C-42, C-41). Si el turno se
 * canceló, ya empezó o cambió de inicio, o el paciente ya no tiene email, devuelve vacío y el envío se descarta.
 */
abstract class DatosTurnoConEnlace implements ReconstructorDatosNotificacion {
    private final TipoNotificacion tipo;
    private final TurnosParaRecordatorio turnos;
    private final PacienteConsulta pacientes;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final ServicioTokenTurno tokens;
    private final Clock reloj;
    private final String urlPublica;

    DatosTurnoConEnlace(TipoNotificacion tipo, TurnosParaRecordatorio turnos, PacienteConsulta pacientes,
            ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales, ServicioTokenTurno tokens, Clock reloj,
            String urlPublica) {
        this.tipo = tipo;
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
        return notificacion.tipo() == tipo;
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
