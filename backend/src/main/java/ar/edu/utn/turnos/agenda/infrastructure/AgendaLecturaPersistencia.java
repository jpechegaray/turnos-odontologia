package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.pacientes.application.TurnosConPacienteConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Lectura de turnos para la agenda (C-28) y para autorizar la ficha del odontólogo. */
@Repository
public class AgendaLecturaPersistencia implements TurnosConPacienteConsulta {
    private final AgendaLecturaJpaRepository turnos;

    AgendaLecturaPersistencia(AgendaLecturaJpaRepository turnos) {
        this.turnos = turnos;
    }

    /** Turnos del período, de cualquier estado; {@code null} en un filtro significa sin filtro. */
    @Transactional(readOnly = true)
    public List<Turno> turnosEnPeriodo(Intervalo periodo, Long profesionalId, Long sillonId) {
        return turnos.enPeriodo(periodo.inicio(), periodo.fin(), profesionalId, sillonId).stream()
                .map(TurnoJpa::dominio).toList();
    }

    /** El paciente tiene algún turno, en cualquier estado, con ese profesional. */
    @Override
    @Transactional(readOnly = true)
    public boolean existeTurno(Long pacienteId, Long profesionalId) {
        return turnos.existsByPacienteIdAndProfesionalId(pacienteId, profesionalId);
    }
}
