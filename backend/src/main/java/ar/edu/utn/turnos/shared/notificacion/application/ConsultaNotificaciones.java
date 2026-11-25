package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.paginacion.Pagina;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del registro de notificaciones (C-25, US-020): solo lectura, para {@code ADMIN} y
 * {@code RECEPCION}. No toca el ciclo de envío ni los reintentos de {@link ServicioNotificaciones}.
 */
@Service
public class ConsultaNotificaciones {

    private final NotificacionRepositorio notificaciones;

    public ConsultaNotificaciones(NotificacionRepositorio notificaciones) {
        this.notificaciones = notificaciones;
    }

    /** Página del registro, de la más reciente a la más antigua; {@code estado} y {@code turnoId} son opcionales. */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional(readOnly = true)
    public Pagina<NotificacionEnviada> listar(EstadoNotificacion estado, Long turnoId, int pagina, int tamanio) {
        return notificaciones.buscar(estado, turnoId, pagina, tamanio);
    }
}
