package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data de {@link NotificacionEnviadaJpa}. La consulta del planificador usa
 * {@code FOR UPDATE SKIP LOCKED} para que dos ejecuciones concurrentes no reclamen la misma fila.
 */
interface NotificacionEnviadaJpaRepository extends JpaRepository<NotificacionEnviadaJpa, Long> {

    @Query(value = """
            SELECT n.* FROM notificacion_enviada n
            WHERE n.estado IN ('PENDIENTE', 'FALLIDA')
              AND n.programada_para IS NOT NULL
              AND n.programada_para <= :ahora
              AND (n.estado = 'PENDIENTE' OR n.intentos < :maximoIntentos)
              AND NOT (n.tipo = 'OFERTA_HUECO'
                       AND n.inicio_referencia IS NOT NULL
                       AND n.inicio_referencia <= :ahora)
            ORDER BY n.programada_para
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificacionEnviadaJpa> reclamarVencidas(
            @Param("ahora") OffsetDateTime ahora,
            @Param("maximoIntentos") int maximoIntentos);
}
