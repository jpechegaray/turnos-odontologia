package ar.edu.utn.turnos.profesionales.bloqueo.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BloqueoJpaRepository extends JpaRepository<Bloqueo, Long> {
    List<Bloqueo> findByProfesionalIdOrderByInicioAscIdAsc(Long profesionalId);

    @Modifying
    @Query("delete from Bloqueo b where b.profesionalId = :profesionalId and b.id = :id")
    int eliminar(@Param("profesionalId") Long profesionalId, @Param("id") Long id);

    @Query("""
            select case when count(b) > 0 then true else false end from Bloqueo b
            where b.profesionalId = :profesionalId and b.inicio < :fin and b.fin > :inicio
            """)
    boolean solapa(@Param("profesionalId") Long profesionalId,
            @Param("inicio") Instant inicio, @Param("fin") Instant fin);

    /** Mismo criterio estricto que {@code solapa}; {@code profesionalId} nulo significa todos (C-28). */
    @Query("""
            select b from Bloqueo b
            where (:profesionalId is null or b.profesionalId = :profesionalId)
              and b.inicio < :fin and b.fin > :inicio
            order by b.inicio asc, b.id asc
            """)
    List<Bloqueo> queSolapan(@Param("profesionalId") Long profesionalId,
            @Param("inicio") Instant inicio, @Param("fin") Instant fin);
}
