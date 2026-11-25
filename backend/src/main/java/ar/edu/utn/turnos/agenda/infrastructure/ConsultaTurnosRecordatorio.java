package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.application.TurnosParaRecordatorio;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ConsultaTurnosRecordatorio implements TurnosParaRecordatorio {
    private final EntityManager em;
    private final TurnosPersistencia turnos;

    ConsultaTurnosRecordatorio(EntityManager em, TurnosPersistencia turnos) {
        this.em = em;
        this.turnos = turnos;
    }

    @Override
    public List<Candidato> proximos(Instant ahora, Instant limite) {
        return em.createQuery("""
                select t.id, t.pacienteId, t.inicio
                from TurnoJpa t where t.estado in :estados and t.inicio > :ahora and t.inicio <= :limite
                """, Object[].class)
                .setParameter("estados", List.of(EstadoTurno.PROGRAMADO, EstadoTurno.CONFIRMADO))
                .setParameter("ahora", ahora).setParameter("limite", limite).getResultList().stream()
                .map(fila -> new Candidato((Long) fila[0], (Long) fila[1], (Instant) fila[2])).toList();
    }

    @Override
    public Optional<Turno> bloquear(Long turnoId) {
        return turnos.buscarParaActualizar(turnoId);
    }
}
