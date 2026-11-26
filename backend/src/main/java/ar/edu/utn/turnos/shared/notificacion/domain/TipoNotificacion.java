package ar.edu.utn.turnos.shared.notificacion.domain;

/**
 * Tipo de email transaccional, según la KB 04. El consumidor elige el tipo; C-15 no decide qué
 * evento lo dispara (eso llega en C-25, C-36, C-37, C-41 y C-44).
 *
 * <p>{@code OFERTA_HUECO} se asocia a una oferta de la lista de espera y no a un turno; por eso su
 * idempotencia se apoya en {@code oferta_hueco_id} y no en {@code turno_id}.
 */
public enum TipoNotificacion {
    RECORDATORIO,
    CONFIRMACION_RESERVA,
    CANCELACION,
    REPROGRAMACION,
    OFERTA_HUECO,
    INGRESO_LISTA_ESPERA,
    CONFIRMACION_LISTA_ESPERA
}
