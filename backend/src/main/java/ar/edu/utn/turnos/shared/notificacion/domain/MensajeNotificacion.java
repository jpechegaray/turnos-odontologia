package ar.edu.utn.turnos.shared.notificacion.domain;

/**
 * Mensaje ya armado que se entrega al puerto de envío. Es un value object plano: no lleva
 * entidades JPA ni conoce el consultorio. El asunto y el cuerpo los arma {@code PlantillaNotificacion}
 * en la capa de aplicación a partir de los datos que aporta el consumidor.
 */
public record MensajeNotificacion(String destinatario, String asunto, String cuerpo) {
    @Override
    public String toString() {
        return "MensajeNotificacion[contenido omitido]";
    }
}
