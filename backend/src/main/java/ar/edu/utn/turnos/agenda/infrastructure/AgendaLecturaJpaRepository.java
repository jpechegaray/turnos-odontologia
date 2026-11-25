package ar.edu.utn.turnos.agenda.infrastructure;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de lectura de la agenda (C-28). Vive aparte de {@link TurnoJpaRepository} para no tocar el
 * archivo que usan los changes de escritura de turnos.
 */
interface AgendaLecturaJpaRepository extends JpaRepository<TurnoJpa, Long> {

    /** Turnos de cualquier estado que comparten algún instante con {@code [inicio, fin)} (RN-02). */
    @Query("""
            select t from TurnoJpa t
            where t.inicio < :fin and t.fin > :inicio
              and (:profesionalId is null or t.profesionalId = :profesionalId)
              and (:sillonId is null or t.sillonId = :sillonId)
            order by t.inicio asc, t.id asc
            """)
    List<TurnoJpa> enPeriodo(@Param("inicio") Instant inicio, @Param("fin") Instant fin,
            @Param("profesionalId") Long profesionalId, @Param("sillonId") Long sillonId);

    boolean existsByPacienteIdAndProfesionalId(Long pacienteId, Long profesionalId);
}
