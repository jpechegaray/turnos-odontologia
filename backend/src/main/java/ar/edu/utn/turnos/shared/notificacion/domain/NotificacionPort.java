package ar.edu.utn.turnos.shared.notificacion.domain;

/**
 * Contrato de envío de emails transaccionales. Es un contrato transversal (vive en {@code shared})
 * que consumirán agenda, reserva pública, lista de espera y los avisos de hueco sin acoplarse a SMTP.
 *
 * <p>El consumidor aporta los datos del email y el servicio arma el mensaje; el puerto recibe un
 * {@link MensajeNotificacion} plano y nunca entidades JPA, para que un cambio de proveedor no obligue
 * a tocar los módulos de dominio. La implementación concreta (SMTP o memoria) se elige por
 * configuración, no por compilación.
 */
public interface NotificacionPort {

    /**
     * Entrega el mensaje al proveedor de correo.
     *
     * @param mensaje destinatario, asunto y cuerpo ya armados
     * @throws ErrorEnvioNotificacion si el proveedor rechaza el envío; el servicio lo registra y reintenta
     */
    void enviar(MensajeNotificacion mensaje) throws ErrorEnvioNotificacion;
}
