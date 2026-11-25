package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA de {@link TurnosActivosConsulta}. Consulta únicamente los campos operativos
 * necesarios de {@link TurnoJpa} mediante JPQL parametrizado (RN-17, RN-35, RN-37, RM-04).
 */
@Repository
@Transactional(readOnly = true)
public class TurnosActivosPersistencia implements TurnosActivosConsulta {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS = List.of(EstadoTurno.PROGRAMADO, EstadoTurno.CONFIRMADO);

    private final EntityManager em;
    private final Clock reloj;

    public TurnosActivosPersistencia(EntityManager em, Clock reloj) {
        this.em = Objects.requireNonNull(em, "em");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public List<TurnoEnConflicto> enIntervaloDeProfesional(Long profesionalId, Intervalo intervalo) {
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(intervalo, "intervalo");
        var consulta = em.createQuery("""
                select t.id, t.inicio, t.fin, t.estado
                from TurnoJpa t
                where t.profesionalId = :profesionalId
                  and t.estado in :activos
                  and t.inicio < :fin
                  and t.fin > :inicio
                order by t.inicio asc, t.id asc
                """, Object[].class)
                .setParameter("profesionalId", profesionalId)
                .setParameter("activos", ESTADOS_ACTIVOS)
                .setParameter("inicio", intervalo.inicio())
                .setParameter("fin", intervalo.fin());
        return ejecutarConsulta(consulta);
    }

    @Override
    public List<TurnoEnConflicto> futurosDeProfesional(Long profesionalId) {
        return futurosPor("profesionalId", profesionalId);
    }

    @Override
    public List<TurnoEnConflicto> futurosDeSillon(Long sillonId) {
        return futurosPor("sillonId", sillonId);
    }

    @Override
    public List<TurnoEnConflicto> futurosDePrestacion(Long prestacionId) {
        return futurosPor("prestacionId", prestacionId);
    }

    private List<TurnoEnConflicto> futurosPor(String campoRecurso, Long recursoId) {
        Objects.requireNonNull(recursoId, campoRecurso);
        Instant ahora = Instant.now(reloj);
        var consulta = em.createQuery("""
                select t.id, t.inicio, t.fin, t.estado
                from TurnoJpa t
                where t.%s = :recursoId
                  and t.estado in :activos
                  and t.fin > :ahora
                order by t.inicio asc, t.id asc
                """.formatted(campoRecurso), Object[].class)
                .setParameter("recursoId", recursoId)
                .setParameter("activos", ESTADOS_ACTIVOS)
                .setParameter("ahora", ahora);
        return ejecutarConsulta(consulta);
    }

    private static List<TurnoEnConflicto> ejecutarConsulta(TypedQuery<Object[]> consulta) {
        return consulta.getResultList().stream()
                .map(fila -> new TurnoEnConflicto(
                        (Long) fila[0],
                        (Instant) fila[1],
                        (Instant) fila[2],
                        ((EstadoTurno) fila[3]).name()))
                .toList();
    }
}
