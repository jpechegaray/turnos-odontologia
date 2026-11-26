package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.listaespera.application.BuscadorCompatibles;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.Hueco;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ofertas de hueco de la lista de espera (C-36; RN-23, RN-38, SU-14): creación con aviso por email, vencimiento y
 * búsqueda por token. El reintento del aviso lo resuelve {@link ReconstructorAvisoOferta}. No chequea rol: lo usan
 * un oyente y planificadores. Los logs llevan solo ids y cantidades; el token en claro solo viaja en el enlace.
 */
@Service
public class OfertasHueco {
    private static final Logger LOG = LoggerFactory.getLogger(OfertasHueco.class);
    private static final Pattern FORMATO_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final ServicioToken tokens = new ServicioToken();
    private final BuscadorCompatibles buscador;
    private final OfertasHuecoPersistencia ofertas;
    private final PacienteConsulta pacientes;
    private final ContenidoAvisoOferta contenido;
    private final ServicioNotificaciones notificaciones;
    private final Clock reloj;
    private final TransactionTemplate transaccionNueva;

    public OfertasHueco(BuscadorCompatibles buscador, OfertasHuecoPersistencia ofertas, PacienteConsulta pacientes,
            ContenidoAvisoOferta contenido, ServicioNotificaciones notificaciones, Clock reloj,
            PlatformTransactionManager transacciones) {
        this.buscador = buscador;
        this.ofertas = ofertas;
        this.pacientes = pacientes;
        this.contenido = contenido;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
        this.transaccionNueva = new TransactionTemplate(transacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Crea una oferta por entrada elegida y avisa a cada paciente (una sola tanda por hueco). Corre en una
     * transacción propia: se llama después del commit de la cancelación. Cada oferta se confirma sola antes de su
     * email, así un duplicado o un fallo en una entrada no afecta a las demás.
     *
     * @return cantidad de ofertas creadas
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int crearYAvisar(Long profesionalId, Instant inicio, Instant fin, Long sillonSugeridoId) {
        var hueco = new Hueco(profesionalId, inicio, fin);
        var elegidas = buscador.aAvisar(hueco);
        if (elegidas.isEmpty()) {
            return 0;
        }
        Map<Long, Paciente> fichas = pacientes.porIds(
                elegidas.stream().map(EntradaListaEspera::pacienteId).collect(Collectors.toSet()));
        var profesional = contenido.nombreProfesional(profesionalId);
        Map<Long, String> nombresPrestacion = new HashMap<>();
        int creadas = 0;
        for (var entrada : elegidas) {
            var paciente = fichas.get(entrada.pacienteId());
            if (paciente == null || paciente.email() == null || paciente.email().isBlank()) {
                continue;
            }
            var generado = tokens.generar();
            var nueva = OfertaHueco.nueva(entrada.id(), profesionalId, inicio, fin, sillonSugeridoId,
                    generado.hash(), Instant.now(reloj));
            OfertaHueco guardada;
            try {
                guardada = transaccionNueva.execute(estado -> ofertas.guardar(nueva));
            } catch (DataIntegrityViolationException repetida) {
                if (OfertasHuecoPersistencia.esOfertaRepetida(repetida)) {
                    continue;
                }
                throw repetida;
            }
            var prestacion = nombresPrestacion.computeIfAbsent(entrada.prestacionId(), contenido::nombrePrestacion);
            notificaciones.registrarYEnviar(TipoNotificacion.OFERTA_HUECO, null, guardada.id(), paciente.id(),
                    contenido.datos(paciente, inicio, prestacion, profesional, generado.claro()));
            creadas++;
        }
        LOG.info("Hueco liberado del profesional {}: {} ofertas creadas de {} entradas elegidas", profesionalId,
                creadas, elegidas.size());
        return creadas;
    }

    /** Pasa a {@code VENCIDA} las ofertas enviadas cuyo hueco ya empezó (RN-24, RN-38). Idempotente. */
    @Transactional
    public int vencerOfertas() {
        int vencidas = ofertas.vencer(Instant.now(reloj));
        if (vencidas > 0) {
            LOG.info("{} ofertas de hueco vencidas", vencidas);
        }
        return vencidas;
    }

    /**
     * Oferta en cualquier estado para el token en claro (C-42): la consulta pública informa también las tomadas y
     * vencidas. Token inexistente o mal formado dan vacío, sin distinguir la causa (RN-38).
     */
    @Transactional(readOnly = true)
    public Optional<OfertaHueco> buscarPorToken(String token) {
        if (token == null || !FORMATO_TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }
        return ofertas.porTokenHash(tokens.hashear(token)).filter(oferta -> tokens.coincide(token, oferta.tokenHash()));
    }

    /**
     * Oferta {@code ENVIADA} y vigente para el token en claro (RN-38). Token inexistente, mal formado,
     * vencido o de una oferta tomada o vencida dan el mismo resultado vacío.
     */
    @Transactional(readOnly = true)
    public Optional<OfertaHueco> buscarVigentePorToken(String token) {
        if (token == null || !FORMATO_TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }
        var ahora = Instant.now(reloj);
        return ofertas.porTokenHash(tokens.hashear(token))
                .filter(oferta -> tokens.coincide(token, oferta.tokenHash()))
                .filter(oferta -> oferta.vigente(ahora));
    }
}
