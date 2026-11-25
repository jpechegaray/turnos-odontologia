package ar.edu.utn.turnos.shared.notificacion.domain;

import ar.edu.utn.turnos.shared.paginacion.Pagina;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistencia del registro de notificaciones. Es un puerto de dominio: no conoce JPA ni Spring.
 * La implementación concreta vive en {@code infrastructure} y es la única que habla con la base.
 */
public interface NotificacionRepositorio {

    /** Inserta o actualiza el registro y devuelve su estado persistido. */
    NotificacionEnviada guardar(NotificacionEnviada notificacion);

    Optional<NotificacionEnviada> porId(Long id);

    /**
     * Reclama, dentro de la transacción actual, las notificaciones vencidas en {@code ahora} que el
     * planificador puede procesar: {@code PENDIENTE} o {@code FALLIDA} con menos de
     * {@code maximoIntentos}. Las filas quedan bloqueadas con {@code FOR UPDATE SKIP LOCKED} para que
     * dos ejecuciones concurrentes no las procesen a la vez.
     */
    List<NotificacionEnviada> reclamarVencidas(Instant ahora, int maximoIntentos);

    /**
     * Página del registro para su consulta (C-25), de la más reciente a la más antigua. Los filtros
     * {@code estado} y {@code turnoId} son opcionales ({@code null} = sin filtro) y se combinan; los
     * totales de la página reflejan los filtros.
     */
    Pagina<NotificacionEnviada> buscar(EstadoNotificacion estado, Long turnoId, int pagina, int tamanio);
}
