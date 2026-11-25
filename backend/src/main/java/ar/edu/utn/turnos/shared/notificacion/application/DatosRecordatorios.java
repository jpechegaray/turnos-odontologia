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

/** Reintento del recordatorio de turno (C-37, RN-29, RN-30). */
@Component
class DatosRecordatorios extends DatosTurnoConEnlace {

    DatosRecordatorios(TurnosParaRecordatorio turnos, PacienteConsulta pacientes, ConsultaPrestaciones prestaciones,
            ProfesionalConsulta profesionales, ServicioTokenTurno tokens, Clock reloj,
            @Value("${APP_PUBLIC_URL:http://localhost:5173}") String urlPublica) {
        super(TipoNotificacion.RECORDATORIO, turnos, pacientes, prestaciones, profesionales, tokens, reloj, urlPublica);
    }
}
