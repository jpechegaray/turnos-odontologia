package ar.edu.utn.turnos.shared.notificacion.domain;

/**
 * Falla al entregar un mensaje al proveedor de correo. El servicio la captura para marcar la
 * notificación como {@code FALLIDA} y programar el reintento; nunca debe llevar el cuerpo del
 * email ni credenciales en el mensaje.
 */
public class ErrorEnvioNotificacion extends RuntimeException {

    public ErrorEnvioNotificacion(String mensaje) {
        super(mensaje);
    }

    public ErrorEnvioNotificacion(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
