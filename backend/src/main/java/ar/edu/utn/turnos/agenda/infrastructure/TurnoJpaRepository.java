package ar.edu.utn.turnos.agenda.infrastructure;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TurnoJpaRepository extends JpaRepository<TurnoJpa, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TurnoJpa t where t.id = :id")
    Optional<TurnoJpa> bloquearPorId(@Param("id") Long id);

    /** {@code SELECT ... FOR UPDATE}: las modificaciones del mismo turno se ordenan (C-27, design D5). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TurnoJpa t where t.id = :id")
    Optional<TurnoJpa> paraActualizar(@Param("id") Long id);

    /**
     * Turnos que ocupan lugar (no CANCELADO, RN-10) del profesional o del sillón y que comparten algún
     * instante con {@code [inicio, fin)}: la misma comparación estricta que {@code Intervalo.solapaCon} (RN-02).
     */
    @Query("""
            select t from TurnoJpa t
            where t.estado <> ar.edu.utn.turnos.agenda.domain.EstadoTurno.CANCELADO
              and (t.profesionalId = :profesionalId or t.sillonId = :sillonId)
              and t.inicio < :fin and t.fin > :inicio
            order by t.inicio asc, t.id asc
            """)
    List<TurnoJpa> activosQueSolapan(@Param("profesionalId") Long profesionalId, @Param("sillonId") Long sillonId,
            @Param("inicio") Instant inicio, @Param("fin") Instant fin);
}
