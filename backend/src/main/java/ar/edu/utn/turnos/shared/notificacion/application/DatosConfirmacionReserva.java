package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.application.TurnosParaRecordatorio;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Reintento de la confirmación de reserva (C-42; también la de la reserva online, C-41; RN-30, RN-31): mismo email
 * con un enlace a un token nuevo del turno.
 */
@Component
class DatosConfirmacionReserva extends DatosTurnoConEnlace {

    DatosConfirmacionReserva(TurnosParaRecordatorio turnos, PacienteConsulta pacientes, ConsultaPrestaciones prestaciones,
            ProfesionalConsulta profesionales, ServicioTokenTurno tokens, Clock reloj,
            @Value("${APP_PUBLIC_URL:http://localhost:5173}") String urlPublica) {
        super(TipoNotificacion.CONFIRMACION_RESERVA, turnos, pacientes, prestaciones, profesionales, tokens, reloj, urlPublica);
    }
}
