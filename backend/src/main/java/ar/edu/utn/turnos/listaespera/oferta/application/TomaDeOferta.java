package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.agenda.application.CrearTurnoOnlineService;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consulta y toma de una oferta de hueco por su token (C-42, US-022, Flujo 7; RN-15, RN-24, RN-25, RN-38).
 *
 * <p>La toma es atómica: en una sola transacción bloquea el profesional, la oferta y la entrada (en ese orden), crea el
 * turno con el alta online de la agenda (que bloquea profesional y sillón), asigna la entrada, pasa a {@code TOMADA} las ofertas
 * del hueco y emite el token del turno. Si dos pacientes confirman a la vez, se ordenan en el lock del profesional y
 * la segunda choca con el turno de la primera; la restricción {@code EXCLUDE} es la garantía final. El email de
 * confirmación sale después del commit. No chequea rol: el token identifica al paciente. Los logs llevan solo ids.
 */
@Service
public class TomaDeOferta {
    private static final Logger LOG = LoggerFactory.getLogger(TomaDeOferta.class);

    private final OfertasHueco ofertas;
    private final OfertasHuecoPersistencia persistencia;
    private final ListaEsperaPersistencia entradas;
    private final CrearTurnoOnlineService altaOnline;
    private final ServicioTokenTurno tokensTurno;
    private final ServicioNotificaciones notificaciones;
    private final PacienteConsulta pacientes;
    private final ConsultaPrestaciones prestaciones;
    private final ContenidoAvisoOferta contenido;
    private final Clock reloj;
    private final TransactionTemplate transaccion;

    TomaDeOferta(OfertasHueco ofertas, OfertasHuecoPersistencia persistencia, ListaEsperaPersistencia entradas,
            CrearTurnoOnlineService altaOnline, ServicioTokenTurno tokensTurno, ServicioNotificaciones notificaciones,
            PacienteConsulta pacientes, ConsultaPrestaciones prestaciones, ContenidoAvisoOferta contenido, Clock reloj,
            PlatformTransactionManager transacciones) {
        this.ofertas = ofertas;
        this.persistencia = persistencia;
        this.entradas = entradas;
        this.altaOnline = altaOnline;
        this.tokensTurno = tokensTurno;
        this.notificaciones = notificaciones;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.contenido = contenido;
        this.reloj = reloj;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    /** Hueco ofrecido y su estado visible; no cambia nada (Flujo 7 paso 1). */
    @Transactional(readOnly = true)
    public OfertaPublica consultar(String token) {
        var oferta = ofertas.buscarPorToken(token).orElseThrow(ErroresOferta::noEncontrada);
        var entrada = entradas.buscar(oferta.listaEsperaId()).orElseThrow(ErroresOferta::noEncontrada);
        var prestacion = prestaciones.buscarPorId(entrada.prestacionId());
        var fin = prestacion.map(PrestacionResumen::duracionDefaultMin).filter(minutos -> minutos > 0)
                .map(minutos -> oferta.huecoInicio().plus(Duration.ofMinutes(minutos)))
                .orElse(oferta.huecoFin());
        return new OfertaPublica(oferta.huecoInicio(), fin, contenido.nombreProfesional(oferta.profesionalId()),
                prestacion.map(PrestacionResumen::nombre).orElse(null), estadoVisible(oferta, entrada, reloj.instant()));
    }

    /**
     * Crea el turno del hueco para el paciente de la entrada (Flujo 7 pasos 3 a 6).
     *
     * @throws ReglaNegocioException 404 {@code OFERTA_NO_ENCONTRADA}; 409 {@code HUECO_YA_TOMADO},
     *         {@code OFERTA_VENCIDA} u {@code OFERTA_NO_DISPONIBLE}
     */
    public TurnoTomado tomar(String token) {
        var oferta = ofertas.buscarPorToken(token).orElseThrow(ErroresOferta::noEncontrada);
        Toma toma;
        try {
            toma = transaccion.execute(estado -> tomarEnTransaccion(oferta));
        } catch (ReglaNegocioException rechazo) {
            var error = traducir(rechazo);
            registrarRechazo(error, oferta);
            throw error;
        }
        LOG.info("Oferta {} tomada: turno {} para la entrada {}", oferta.id(), toma.turno().id(),
                oferta.listaEsperaId());
        enviarConfirmacion(toma);
        return new TurnoTomado(toma.turno().intervalo().inicio(), toma.turno().intervalo().fin(),
                toma.turno().estado(), toma.profesional(), toma.prestacion(), toma.token());
    }

    private Toma tomarEnTransaccion(OfertaHueco leida) {
        // Orden de locks: profesional → oferta → entrada → (profesional, sillón). Dos tomas del mismo hueco se
        // ordenan en el primero; así ninguna espera una fila de oferta que tiene la otra (design D3).
        altaOnline.ordenarPorProfesional(leida.profesionalId());
        var oferta = persistencia.bloquear(leida.id()).orElseThrow(ErroresOferta::noEncontrada);
        if (oferta.estado() == EstadoOferta.TOMADA) {
            throw ErroresOferta.huecoYaTomado();
        }
        if (oferta.estado() == EstadoOferta.VENCIDA || !reloj.instant().isBefore(oferta.huecoInicio())) {
            throw ErroresOferta.vencida();
        }
        var entrada = entradas.bloquear(oferta.listaEsperaId())
                .filter(e -> e.estado() == EstadoListaEspera.ACTIVA)
                .orElseThrow(ErroresOferta::noDisponible);
        // RN-24 y SU-14: inicio del hueco, duración por defecto, sillón del turno liberado o el primero libre.
        Turno turno = altaOnline.crear(entrada.pacienteId(), oferta.profesionalId(), entrada.prestacionId(),
                oferta.huecoInicio(), oferta.sillonSugeridoId());
        entradas.guardar(entrada.asignar(turno.id()));
        persistencia.tomarOfertasDelHueco(oferta.profesionalId(), oferta.huecoInicio(), oferta.huecoFin());
        var emitido = tokensTurno.emitir(turno.id());
        return new Toma(turno, entrada.pacienteId(), contenido.nombreProfesional(oferta.profesionalId()),
                contenido.nombrePrestacion(entrada.prestacionId()), emitido.claro());
    }

    /**
     * Traduce el rechazo de la agenda al lenguaje de la oferta (design D4): el profesional ocupado es "hueco ya
     * tomado", RN-13 es "vencida" y cualquier otra regla del alta deja la oferta "no disponible".
     */
    private static ReglaNegocioException traducir(ReglaNegocioException rechazo) {
        if (ErroresOferta.esPropio(rechazo)) {
            return rechazo;
        }
        return switch (rechazo.codigo()) {
            case "RN-01" -> "PROFESIONAL".equals(rechazo.propiedades().get("recurso"))
                    ? ErroresOferta.huecoYaTomado() : ErroresOferta.noDisponible();
            case "RN-13" -> ErroresOferta.vencida();
            default -> ErroresOferta.noDisponible();
        };
    }

    /** Después del rollback deja la oferta con su estado real, para que el enlace lo informe. La entrada no cambia. */
    private void registrarRechazo(ReglaNegocioException error, OfertaHueco oferta) {
        switch (error.codigo()) {
            case ErroresOferta.HUECO_YA_TOMADO -> transaccion.executeWithoutResult(estado ->
                    persistencia.tomarOfertasDelHueco(oferta.profesionalId(), oferta.huecoInicio(), oferta.huecoFin()));
            case ErroresOferta.VENCIDA -> transaccion.executeWithoutResult(estado ->
                    persistencia.vencerOferta(oferta.id()));
            default -> {
                // OFERTA_NO_DISPONIBLE: la oferta sigue ENVIADA; el enlace informa NO_DISPONIBLE por la entrada.
            }
        }
        LOG.info("Oferta {} rechazada al tomarla: {}", oferta.id(), error.codigo());
    }

    /** RN-31 y RN-30: si el correo falla, la notificación queda FALLIDA y el turno se conserva. */
    private void enviarConfirmacion(Toma toma) {
        try {
            pacientes.porId(toma.pacienteId())
                    .filter(paciente -> paciente.email() != null && !paciente.email().isBlank())
                    .ifPresent(paciente -> notificaciones.registrarYEnviar(TipoNotificacion.CONFIRMACION_RESERVA,
                            toma.turno().id(), null, paciente.id(), contenido.confirmacion(paciente,
                                    toma.turno().intervalo().inicio(), toma.prestacion(), toma.profesional(),
                                    toma.token())));
        } catch (RuntimeException fallo) {
            LOG.warn("No se pudo registrar la confirmación del turno {}: {}", toma.turno().id(),
                    fallo.getClass().getSimpleName());
        }
    }

    static OfertaPublica.Estado estadoVisible(OfertaHueco oferta, EntradaListaEspera entrada, Instant ahora) {
        if (oferta.estado() == EstadoOferta.TOMADA) {
            return OfertaPublica.Estado.TOMADA;
        }
        if (oferta.estado() == EstadoOferta.VENCIDA || !ahora.isBefore(oferta.huecoInicio())) {
            return OfertaPublica.Estado.VENCIDA;
        }
        return Optional.ofNullable(entrada).map(EntradaListaEspera::estado).orElse(null) == EstadoListaEspera.ACTIVA
                ? OfertaPublica.Estado.DISPONIBLE : OfertaPublica.Estado.NO_DISPONIBLE;
    }

    private record Toma(Turno turno, Long pacienteId, String profesional, String prestacion, String token) {
        @Override
        public String toString() {
            return "Toma[turno=" + turno.id() + ", token=***]";
        }
    }
}
