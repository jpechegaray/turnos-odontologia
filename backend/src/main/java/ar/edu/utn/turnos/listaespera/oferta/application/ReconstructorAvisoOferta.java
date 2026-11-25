package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.shared.notificacion.application.DatosNotificacion;
import ar.edu.utn.turnos.shared.notificacion.application.ReconstructorDatosNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contenido del reintento de un aviso de oferta (design D5). El token en claro no se guarda, así que rota el token
 * de la oferta y arma el enlace nuevo; el anterior nunca llegó al paciente. Si la oferta ya no está vigente o el
 * paciente no tiene email, no aporta datos y pide descartar ({@link #descartarSiFaltanDatos}): el reintento queda
 * terminal sin enviar, en vez de mandar un email sin enlace. Corre dentro de la transacción del planificador de
 * notificaciones y bloquea la fila de la oferta.
 *
 * <p>No depende de {@link OfertasHueco}: este reconstructor es una dependencia de {@code ServicioNotificaciones},
 * que a su vez usa {@link OfertasHueco}.
 */
@Component
class ReconstructorAvisoOferta implements ReconstructorDatosNotificacion {
    private final ServicioToken tokens = new ServicioToken();
    private final OfertasHuecoPersistencia ofertas;
    private final ListaEsperaPersistencia entradas;
    private final PacienteConsulta pacientes;
    private final ContenidoAvisoOferta contenido;
    private final Clock reloj;

    ReconstructorAvisoOferta(OfertasHuecoPersistencia ofertas, ListaEsperaPersistencia entradas,
            PacienteConsulta pacientes, ContenidoAvisoOferta contenido, Clock reloj) {
        this.ofertas = ofertas;
        this.entradas = entradas;
        this.pacientes = pacientes;
        this.contenido = contenido;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Optional<DatosNotificacion> datosPara(NotificacionEnviada notificacion) {
        if (!esAvisoDeOferta(notificacion)) {
            return Optional.empty();
        }
        var oferta = ofertas.bloquear(notificacion.ofertaHuecoId()).filter(o -> o.vigente(Instant.now(reloj)));
        var paciente = pacientes.porId(notificacion.pacienteId())
                .filter(p -> p.email() != null && !p.email().isBlank());
        if (oferta.isEmpty() || paciente.isEmpty()) {
            return Optional.empty();
        }
        var generado = tokens.generar();
        ofertas.guardar(oferta.get().conToken(generado.hash()));
        var prestacionId = entradas.buscar(oferta.get().listaEsperaId()).map(EntradaListaEspera::prestacionId)
                .orElse(null);
        return Optional.of(contenido.datos(paciente.get(), oferta.get().huecoInicio(),
                contenido.nombrePrestacion(prestacionId), contenido.nombreProfesional(oferta.get().profesionalId()),
                generado.claro()));
    }

    /** Un aviso de oferta sin datos vigentes no se envía (RN-38): sin enlace válido no sirve. */
    @Override
    public boolean descartarSiFaltanDatos(NotificacionEnviada notificacion) {
        return esAvisoDeOferta(notificacion);
    }

    private static boolean esAvisoDeOferta(NotificacionEnviada notificacion) {
        return notificacion.tipo() == TipoNotificacion.OFERTA_HUECO && notificacion.ofertaHuecoId() != null;
    }
}
