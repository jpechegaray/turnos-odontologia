package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.application.TurnosParaRecordatorio;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.ColaRecordatorios;
import java.time.Clock;
import java.time.Duration;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorRecordatorios {
    private final TurnosParaRecordatorio turnos;
    private final PacienteConsulta pacientes;
    private final ConfiguracionServicio configuracion;
    private final ColaRecordatorios cola;
    private final ServicioNotificaciones notificaciones;
    private final Clock reloj;

    PlanificadorRecordatorios(TurnosParaRecordatorio turnos, PacienteConsulta pacientes,
            ConfiguracionServicio configuracion, ColaRecordatorios cola, ServicioNotificaciones notificaciones,
            Clock reloj) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.configuracion = configuracion;
        this.cola = cola;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    public void procesar() {
        var ahora = reloj.instant();
        var candidatos = turnos.proximos(ahora, ahora.plus(Duration.ofHours(configuracion.anticipacionRecordatorioHoras())));
        var fichas = pacientes.porIds(candidatos.stream().map(TurnosParaRecordatorio.Candidato::pacienteId)
                .collect(Collectors.toSet()));
        for (var candidato : candidatos) {
            Paciente paciente = fichas.get(candidato.pacienteId());
            if (paciente != null && paciente.email() != null && !paciente.email().isBlank()) {
                cola.encolar(candidato.turnoId(), candidato.pacienteId(), paciente.email(), candidato.inicio(), ahora);
            }
        }
        notificaciones.procesarVencidas();
    }
}
