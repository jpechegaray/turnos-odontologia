package ar.edu.utn.turnos.agenda.infrastructure;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Una sola lectura de ocupación del profesional o de los sillones candidatos, sin datos personales. */
@Repository
public class ConsultaOcupacionDisponibilidad {
    private final EntityManager em;

    public ConsultaOcupacionDisponibilidad(EntityManager em) {
        this.em = em;
    }

    public List<TurnoOcupado> buscar(Long profesionalId, List<Long> sillones, Intervalo rango) {
        if (sillones.isEmpty()) {
            return List.of();
        }
        return em.createQuery("""
                select t.id as id, t.profesionalId as profesional, t.sillonId as sillon,
                       t.inicio as inicio, t.fin as fin, t.estado as estado
                from TurnoJpa t
                where t.estado <> :cancelado and t.inicio < :hasta and t.fin > :desde
                  and (t.profesionalId = :profesional or t.sillonId in :sillones)
                """, Tuple.class)
                .setParameter("cancelado", EstadoTurno.CANCELADO).setParameter("hasta", rango.fin())
                .setParameter("desde", rango.inicio()).setParameter("profesional", profesionalId)
                .setParameter("sillones", sillones).getResultList().stream()
                .map(t -> new TurnoOcupado(t.get("id", Long.class), t.get("profesional", Long.class),
                        t.get("sillon", Long.class), new Intervalo(t.get("inicio", Instant.class), t.get("fin", Instant.class)),
                        t.get("estado", EstadoTurno.class))).toList();
    }
}
