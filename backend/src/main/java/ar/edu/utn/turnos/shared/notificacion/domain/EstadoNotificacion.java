package ar.edu.utn.turnos.shared.notificacion.domain;

/**
 * Estado del registro de una notificación, según la KB 04.
 *
 * <p>{@code PENDIENTE} es una notificación registrada que todavía no se envió (o que espera su
 * momento programado); {@code ENVIADA} es terminal y guarda la fecha de envío; {@code FALLIDA}
 * conserva el último error y puede reintentarse hasta el máximo configurado.
 */
public enum EstadoNotificacion {
    PENDIENTE,
    ENVIADA,
    FALLIDA
}
