package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.Turno;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TurnosParaRecordatorio {
    List<Candidato> proximos(Instant ahora, Instant limite);
    Optional<Turno> bloquear(Long turnoId);

    record Candidato(Long turnoId, Long pacienteId, Instant inicio) {}
}
