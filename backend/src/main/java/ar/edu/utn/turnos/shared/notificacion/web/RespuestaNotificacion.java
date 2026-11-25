package ar.edu.utn.turnos.shared.notificacion.web;

import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Instant;

/**
 * Un elemento del registro de notificaciones. {@code ultimoError} solo tiene valor en las
 * {@code FALLIDA}: es el mensaje que registró el envío, sin trazas del servidor.
 */
record RespuestaNotificacion(
        Long id,
        TipoNotificacion tipo,
        EstadoNotificacion estado,
        String canal,
        String destinatario,
        Long turnoId,
        Long ofertaHuecoId,
        Long pacienteId,
        Instant inicioReferencia,
        int intentos,
        String ultimoError,
        Instant programadaPara,
        Instant enviadaEn) {

    static RespuestaNotificacion de(NotificacionEnviada n) {
        return new RespuestaNotificacion(n.id(), n.tipo(), n.estado(), n.canal(), n.destinatario(), n.turnoId(),
                n.ofertaHuecoId(), n.pacienteId(), n.inicioReferencia(), n.intentos(), n.ultimoError(),
                n.programadaPara(), n.enviadaEn());
    }
}
