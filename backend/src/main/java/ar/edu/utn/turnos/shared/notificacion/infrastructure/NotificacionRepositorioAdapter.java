package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adaptador que implementa {@link NotificacionRepositorio} con JPA. Mantiene el mapeo entre el
 * record de dominio y la entidad, y convierte el instante del reloj a {@link java.time.OffsetDateTime}
 * para la consulta nativa del planificador.
 */
@Component
public class NotificacionRepositorioAdapter implements NotificacionRepositorio {

    private final NotificacionEnviadaJpaRepository jpaRepository;

    public NotificacionRepositorioAdapter(NotificacionEnviadaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificacionEnviada guardar(NotificacionEnviada notificacion) {
        NotificacionEnviadaJpa guardada = jpaRepository.saveAndFlush(
                NotificacionEnviadaJpa.desdeDominio(notificacion));
        return guardada.aDominio();
    }

    @Override
    public Optional<NotificacionEnviada> porId(Long id) {
        return jpaRepository.findById(id).map(NotificacionEnviadaJpa::aDominio);
    }

    @Override
    public List<NotificacionEnviada> reclamarVencidas(Instant ahora, int maximoIntentos) {
        return jpaRepository.reclamarVencidas(ahora.atOffset(ZoneOffset.UTC), maximoIntentos)
                .stream()
                .map(NotificacionEnviadaJpa::aDominio)
                .toList();
    }
}
