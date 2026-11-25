package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.paginacion.Pagina;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Override
    public Pagina<NotificacionEnviada> buscar(EstadoNotificacion estado, Long turnoId, int pagina, int tamanio) {
        Page<NotificacionEnviadaJpa> resultado = jpaRepository.buscar(estado, turnoId, PageRequest.of(pagina, tamanio));
        List<NotificacionEnviada> contenido = resultado.getContent().stream()
                .map(NotificacionEnviadaJpa::aDominio)
                .toList();
        return new Pagina<>(contenido, pagina, tamanio, resultado.getTotalElements());
    }
}
