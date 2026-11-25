package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoConsulta;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Responde las preguntas del validador con los puertos de profesionales (horarios y bloqueos) y la
 * persistencia de turnos. Agenda lee a profesionales solo por estas interfaces (KB 08).
 */
@Component
class ConsultasAgendaAdaptador implements ConsultasAgenda {
    private final HorarioConsulta horarios;
    private final BloqueoConsulta bloqueos;
    private final TurnosPersistencia turnos;

    ConsultasAgendaAdaptador(HorarioConsulta horarios, BloqueoConsulta bloqueos, TurnosPersistencia turnos) {
        this.horarios = horarios;
        this.bloqueos = bloqueos;
        this.turnos = turnos;
    }

    @Override
    public boolean dentroDeHorario(Long profesionalId, Intervalo intervalo) {
        return horarios.contiene(profesionalId, intervalo);
    }

    @Override
    public boolean enBloqueo(Long profesionalId, Intervalo intervalo) {
        return bloqueos.solapa(profesionalId, intervalo);
    }

    @Override
    public List<TurnoOcupado> turnosQueSolapan(Long profesionalId, Long sillonId, Intervalo intervalo) {
        return turnos.turnosQueSolapan(profesionalId, sillonId, intervalo);
    }
}
