package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionPort;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.PropiedadesNotificaciones;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registro idempotente y envío de emails transaccionales, más el procesamiento de los vencidos por el
 * planificador (RN-30, RN-31, DD-07).
 *
 * <p><b>Registro.</b> {@link #registrarYEnviar} persiste un registro {@code PENDIENTE}, programado una
 * espera inicial más adelante para que el planificador no lo duplique mientras se envía, antes de
 * enviar, dentro de una transacción {@code REQUIRES_NEW} que captura {@link DataIntegrityViolationException}:
 * si la fila ya existe (misma clave única), no se reenvía y el error no escapa. Un destinatario nulo o
 * blanco no crea fila ni envía (RN-35).
 *
 * <p><b>Resultado.</b> Un envío exitoso deja la notificación {@code ENVIADA} con {@code enviadaEn}. Un
 * fallo deja {@code FALLIDA} con el último error, incrementa los intentos y reprograma con espera
 * creciente; al alcanzar el máximo configurado queda terminal y no se reintenta. El servicio nunca
 * propaga el fallo: el turno del consumidor no se pierde.
 *
 * <p><b>Planificador.</b> {@link #procesarVencidas} reclama, dentro de una transacción, las filas
 * vencidas con {@code FOR UPDATE SKIP LOCKED} y las procesa en el mismo ciclo; dos ejecuciones
 * concurrentes no duplican un envío. Para reintentar pide los datos a los {@link ReconstructorDatosNotificacion}
 * de los consumidores. Todo instante sale del {@link Clock} inyectable (DD-05).
 */
@Service
public class ServicioNotificaciones {

    private static final String ERROR_POR_DEFECTO = "Error de envío de notificación";

    private final NotificacionRepositorio repositorio;
    private final NotificacionPort puerto;
    private final PlantillaNotificacion plantilla;
    private final PropiedadesNotificaciones propiedades;
    private final Clock reloj;
    private final TransactionTemplate transaccionRegistro;
    private final TransactionTemplate transaccionProcesamiento;
    private final List<ReconstructorDatosNotificacion> reconstructores;

    public ServicioNotificaciones(
            NotificacionRepositorio repositorio,
            NotificacionPort puerto,
            PlantillaNotificacion plantilla,
            PropiedadesNotificaciones propiedades,
            Clock reloj,
            PlatformTransactionManager gestorTransacciones) {
        this(repositorio, puerto, plantilla, propiedades, reloj, gestorTransacciones, List.of());
    }

    @Autowired
    public ServicioNotificaciones(
            NotificacionRepositorio repositorio,
            NotificacionPort puerto,
            PlantillaNotificacion plantilla,
            PropiedadesNotificaciones propiedades,
            Clock reloj,
            PlatformTransactionManager gestorTransacciones,
            List<ReconstructorDatosNotificacion> reconstructores) {
        this.repositorio = repositorio;
        this.reconstructores = List.copyOf(reconstructores);
        this.puerto = puerto;
        this.plantilla = plantilla;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.transaccionRegistro = new TransactionTemplate(gestorTransacciones);
        this.transaccionRegistro.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaccionProcesamiento = new TransactionTemplate(gestorTransacciones);
    }

    /**
     * Registra la notificación como {@code PENDIENTE} y la envía de inmediato. Si el destinatario es
     * nulo o blanco no hace nada; si el registro ya existe no reenvía y no propaga el error.
     */
    public void registrarYEnviar(
            TipoNotificacion tipo,
            Long turnoId,
            Long ofertaHuecoId,
            Long pacienteId,
            DatosNotificacion datos) {
        if (datos == null || datos.destinatario() == null || datos.destinatario().isBlank()) {
            return;
        }
        // El envío inmediato lo hace este hilo. La fila queda programada una espera más adelante para que el
        // planificador no la tome mientras tanto (doble email); solo la retoma si este envío nunca terminó.
        Instant reservaHasta = reloj.instant().plus(propiedades.reintento().esperaInicial());
        NotificacionEnviada pendiente = NotificacionEnviada.pendiente(
                tipo, datos.destinatario(), turnoId, ofertaHuecoId, pacienteId, datos.inicio(), reservaHasta);
        NotificacionEnviada registrada;
        try {
            registrada = transaccionRegistro.execute(status -> repositorio.guardar(pendiente));
        } catch (DataIntegrityViolationException duplicado) {
            return;
        }
        if (registrada == null) {
            return;
        }
        enviarYGuardar(registrada, datos, reloj.instant());
    }

    /**
     * Reclama y procesa las notificaciones vencidas: las {@code PENDIENTE} y las {@code FALLIDA} con
     * intentos disponibles cuyo próximo intento ya pasó. El aviso {@code OFERTA_HUECO} se descarta
     * cuando el hueco ya comenzó (RN-38). La selección y el envío ocurren en una sola transacción.
     */
    public void procesarVencidas() {
        int maximoIntentos = propiedades.reintento().maximoIntentos();
        transaccionProcesamiento.executeWithoutResult(status -> {
            Instant ahora = reloj.instant();
            for (NotificacionEnviada notificacion : repositorio.reclamarVencidas(ahora, maximoIntentos)) {
                var datos = datosParaReintento(notificacion);
                if (datos.isPresent()) {
                    var envio = datos.get();
                    var registro = notificacion.tipo() == TipoNotificacion.RECORDATORIO
                            ? notificacion.dirigidaA(envio.destinatario()) : notificacion;
                    enviarYGuardar(registro, envio, ahora);
                } else {
                    repositorio.guardar(notificacion.descartada("Recordatorio descartado: turno o destinatario no vigente"));
                }
            }
        });
    }

    private void enviarYGuardar(NotificacionEnviada notificacion, DatosNotificacion datos, Instant ahora) {
        NotificacionEnviada resultado = intentarEnvio(notificacion, datos, ahora);
        transaccionProcesamiento.executeWithoutResult(status -> repositorio.guardar(resultado));
    }

    private NotificacionEnviada intentarEnvio(NotificacionEnviada notificacion, DatosNotificacion datos, Instant ahora) {
        try {
            puerto.enviar(plantilla.armar(notificacion.tipo(), datos));
            return notificacion.conEnvioExitoso(ahora);
        } catch (RuntimeException fallo) {
            return registrarFallo(notificacion, fallo, ahora);
        }
    }

    private NotificacionEnviada registrarFallo(NotificacionEnviada notificacion, RuntimeException fallo, Instant ahora) {
        String error = notificacion.tipo() == TipoNotificacion.RECORDATORIO ? ERROR_POR_DEFECTO : mensajeDe(fallo);
        int maximoIntentos = propiedades.reintento().maximoIntentos();
        int intentosResultantes = notificacion.intentos() + 1;
        if (intentosResultantes >= maximoIntentos) {
            return notificacion.conIntentoFallido(error, null);
        }
        Duration espera = NotificacionEnviada.esperaCreciente(
                intentosResultantes,
                propiedades.reintento().esperaInicial(),
                propiedades.reintento().multiplicador());
        return notificacion.conIntentoFallido(error, ahora.plus(espera));
    }

    private static String mensajeDe(RuntimeException fallo) {
        String mensaje = fallo.getMessage();
        return (mensaje == null || mensaje.isBlank()) ? ERROR_POR_DEFECTO : mensaje;
    }

    /**
     * La fila no guarda el cuerpo (puede llevar un enlace con token, RN-07): el consumidor que lo registró
     * vuelve a aportar los datos. Sin consumidor que responda, se envía la versión mínima.
     */
    private Optional<DatosNotificacion> datosParaReintento(NotificacionEnviada notificacion) {
        var datos = reconstructores.stream()
                .map(reconstructor -> reconstructor.datosPara(notificacion))
                .flatMap(java.util.Optional::stream)
                .findFirst();
        if (datos.isPresent() || reconstructores.stream().anyMatch(r -> r.descartarSiFaltanDatos(notificacion))) {
            return datos;
        }
        return Optional.of(new DatosNotificacion(notificacion.destinatario(), null,
                notificacion.inicioReferencia(), null, null, null));
    }
}
