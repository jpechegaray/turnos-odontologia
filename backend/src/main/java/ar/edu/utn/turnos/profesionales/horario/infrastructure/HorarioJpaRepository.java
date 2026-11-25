package ar.edu.utn.turnos.profesionales.horario.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface HorarioJpaRepository extends JpaRepository<HorarioAtencion, Long> {
    List<HorarioAtencion> findByProfesionalIdOrderByDiaSemanaAscHoraInicioAscIdAsc(Long profesionalId);

    List<HorarioAtencion> findAllByOrderByProfesionalIdAscDiaSemanaAscHoraInicioAscIdAsc();

    Optional<HorarioAtencion> findByIdAndProfesionalId(Long id, Long profesionalId);

    boolean existsByIdAndProfesionalId(Long id, Long profesionalId);

    @Modifying
    @Query("delete from HorarioAtencion h where h.profesionalId = :profesionalId and h.id = :id")
    int eliminar(@Param("profesionalId") Long profesionalId, @Param("id") Long id);

    /*
     * Las horas viajan como texto y se castean a time en PostgreSQL: con hibernate.jdbc.time_zone=UTC,
     * un parámetro LocalTime se desplazaría según la zona de la JVM y compararía la hora equivocada.
     */

    /** Superposición semiabierta en el mismo día; {@code excluido} es el tramo que se modifica (o -1). */
    @Query(value = """
            select exists (select 1 from horario_atencion h
            where h.profesional_id = :profesionalId and h.dia_semana = :dia and h.id <> :excluido
              and h.hora_inicio < cast(:fin as time) and h.hora_fin > cast(:inicio as time))
            """, nativeQuery = true)
    boolean existeSuperpuesto(@Param("profesionalId") Long profesionalId, @Param("dia") int dia,
            @Param("inicio") String inicio, @Param("fin") String fin, @Param("excluido") Long excluido);

    @Query(value = """
            select exists (select 1 from horario_atencion h
            where h.profesional_id = :profesionalId and h.dia_semana = :dia
              and h.hora_inicio <= cast(:inicio as time) and h.hora_fin >= cast(:fin as time))
            """, nativeQuery = true)
    boolean contiene(@Param("profesionalId") Long profesionalId, @Param("dia") int dia,
            @Param("inicio") String inicio, @Param("fin") String fin);

    /** Bloquea la fila del profesional para serializar las escrituras de sus tramos; vacío si no existe. */
    @Query(value = "select id from profesional where id = :profesionalId for update", nativeQuery = true)
    Optional<Long> bloquearProfesional(@Param("profesionalId") Long profesionalId);
}
