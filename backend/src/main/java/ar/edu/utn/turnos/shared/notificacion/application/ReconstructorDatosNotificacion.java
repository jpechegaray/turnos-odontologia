package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import java.util.Optional;

/**
 * Lo implementa cada consumidor que registra notificaciones (C-25, C-36, C-37, C-41, C-44) para que un
 * reintento lleve el mismo contenido que el primer envío. La fila no guarda el cuerpo del email porque
 * puede contener un enlace con token, que nunca se persiste en claro (RN-07, regla dura 3).
 */
public interface ReconstructorDatosNotificacion {

    /**
     * Vuelve a armar los datos del email a partir del registro (tipo, turno u oferta, paciente).
     *
     * @return vacío si la notificación no es de este consumidor
     */
    Optional<DatosNotificacion> datosPara(NotificacionEnviada notificacion);

    /** Indica que para este tipo la falta de datos vigentes debe cancelar el envío. */
    default boolean descartarSiFaltanDatos(NotificacionEnviada notificacion) {
        return false;
    }
}
