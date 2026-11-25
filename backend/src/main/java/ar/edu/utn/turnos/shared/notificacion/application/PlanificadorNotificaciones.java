package ar.edu.utn.turnos.shared.notificacion.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Planificador periódico de envíos pendientes y fallidos (Flujo 5). Se activa con
 * {@code app.notificaciones.planificador.habilitado=true} (por defecto) y corre cada
 * {@code app.notificaciones.planificador.intervalo} (5 minutos por defecto). Delega en
 * {@link ServicioNotificaciones#procesarVencidas()}, que reclama las filas vencidas con
 * {@code FOR UPDATE SKIP LOCKED} para que dos ejecuciones concurrentes no dupliquen un envío.
 *
 * <p>{@code @EnableScheduling} vive en {@code ConfiguracionNotificaciones}, no en la clase raíz.
 */
@Component
@ConditionalOnProperty(
        prefix = "app.notificaciones.planificador",
        name = "habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class PlanificadorNotificaciones {

    private final ServicioNotificaciones servicio;

    public PlanificadorNotificaciones(ServicioNotificaciones servicio) {
        this.servicio = servicio;
    }

    /** Dispara un ciclo de procesamiento de notificaciones vencidas. */
    @Scheduled(fixedDelayString = "${app.notificaciones.planificador.intervalo:5m}")
    public void procesar() {
        servicio.procesarVencidas();
    }
}
