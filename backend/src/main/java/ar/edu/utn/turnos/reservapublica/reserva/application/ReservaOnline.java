package ar.edu.utn.turnos.reservapublica.reserva.application;

import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.pacientes.application.AltaPacienteReservaOnline;
import ar.edu.utn.turnos.pacientes.application.PacienteConsulta;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import ar.edu.utn.turnos.reservapublica.application.DisponibilidadPublica;
import ar.edu.utn.turnos.reservapublica.reserva.infrastructure.BloqueoReservaPorEmail;
import ar.edu.utn.turnos.reservapublica.reserva.infrastructure.ConsentimientoTurno;
import ar.edu.utn.turnos.reservapublica.reserva.infrastructure.ConsentimientoTurnoRepositorio;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.notificacion.application.DatosNotificacion;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reserva online de un paciente anónimo (US-015, Flujo 4). No es transaccional por sí misma: valida el
 * consentimiento, los recursos y los límites de RN-27, y abre una transacción que toma el lock del email, aplica
 * el tope, resuelve o crea la ficha (RN-28), crea el turno con las reglas de la agenda, registra el consentimiento
 * y emite el token. El email de confirmación sale recién después del commit, así nunca se avisa un turno que se
 * revirtió; un fallo del envío no afecta la reserva (RN-31).
 */
@Service
public class ReservaOnline {

    /** Las alternativas del 409 (RN-08) se buscan en la semana siguiente al pedido y se ofrecen hasta cinco. */
    static final Duration VENTANA_ALTERNATIVAS = Duration.ofDays(7);
    static final int MAXIMO_ALTERNATIVAS = 5;
    static final String LIMITE_ANTICIPACION = "ANTICIPACION";
    static final String LIMITE_HORIZONTE = "HORIZONTE";
    static final String LIMITE_TOPE_EMAIL = "TOPE_EMAIL";

    private final DisponibilidadPublica disponibilidadPublica;
    private final DisponibilidadService disponibilidad;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final SillonConsulta sillones;
    private final ConfiguracionServicio configuracion;
    private final PacienteConsulta pacientes;
    private final AltaPacienteReservaOnline altaPaciente;
    private final TurnosActivosConsulta turnosActivos;
    private final CrearTurnoService agenda;
    private final BloqueoReservaPorEmail bloqueo;
    private final ConsentimientoTurnoRepositorio consentimientos;
    private final ServicioTokenTurno tokens;
    private final ServicioNotificaciones notificaciones;
    private final TransactionTemplate transaccion;
    private final Clock reloj;
    private final String urlPublica;

    ReservaOnline(DisponibilidadPublica disponibilidadPublica, DisponibilidadService disponibilidad,
            ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales, SillonConsulta sillones,
            ConfiguracionServicio configuracion, PacienteConsulta pacientes, AltaPacienteReservaOnline altaPaciente,
            TurnosActivosConsulta turnosActivos, CrearTurnoService agenda, BloqueoReservaPorEmail bloqueo,
            ConsentimientoTurnoRepositorio consentimientos, ServicioTokenTurno tokens,
            ServicioNotificaciones notificaciones, PlatformTransactionManager transacciones, Clock reloj,
            @Value("${APP_PUBLIC_URL:http://localhost:5173}") String urlPublica) {
        this.disponibilidadPublica = disponibilidadPublica;
        this.disponibilidad = disponibilidad;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.configuracion = configuracion;
        this.pacientes = pacientes;
        this.altaPaciente = altaPaciente;
        this.turnosActivos = turnosActivos;
        this.agenda = agenda;
        this.bloqueo = bloqueo;
        this.consentimientos = consentimientos;
        this.tokens = tokens;
        this.notificaciones = notificaciones;
        this.transaccion = new TransactionTemplate(transacciones);
        this.reloj = reloj;
        this.urlPublica = urlPublica.strip().replaceFirst("/+$", "");
    }

    public ReservaCreada reservar(SolicitudReserva solicitud) {
        ConsentimientoDatos.exigir(solicitud.aceptaTratamientoDatos(), solicitud.versionConsentimiento());
        disponibilidadPublica.validarRecursos(solicitud.profesionalId(), solicitud.prestacionId());
        var prestacion = prestaciones.buscarPorId(solicitud.prestacionId())
                .orElseThrow(() -> new IllegalStateException("La prestación validada ya no existe"));
        var intervalo = Intervalo.desde(solicitud.inicio(), prestacion.duracionDefaultMin());
        var ahora = reloj.instant();
        validarLimites(intervalo, ahora);

        Confirmada confirmada;
        try {
            confirmada = transaccion.execute(estado -> reservarEnTransaccion(solicitud, intervalo, ahora));
        } catch (ReglaNegocioException ex) {
            if ("RN-01".equals(ex.codigo())) {
                throw ocupadoConAlternativas(solicitud);
            }
            throw ex;
        }
        enviarConfirmacionDespuesDelCommit(confirmada, prestacion);
        return new ReservaCreada(intervalo.inicio(), intervalo.fin(), prestacion.nombre(),
                nombreProfesional(solicitud.profesionalId()), confirmada.token());
    }

    /**
     * RN-27 con los mismos bordes que el recorte de la disponibilidad pública: el inicio no es anterior a la
     * anticipación mínima y el turno termina a más tardar en el horizonte. Todo horario ofrecido se puede reservar.
     */
    private void validarLimites(Intervalo intervalo, Instant ahora) {
        var minimo = ahora.plus(Duration.ofHours(configuracion.anticipacionMinimaReservaOnlineHoras()));
        if (intervalo.inicio().isBefore(minimo)) {
            throw limite(LIMITE_ANTICIPACION, "El turno tiene que reservarse con más anticipación");
        }
        var maximo = ahora.plus(Duration.ofDays(configuracion.horizonteReservaOnlineDias()));
        if (intervalo.fin().isAfter(maximo)) {
            throw limite(LIMITE_HORIZONTE, "El turno queda fuera del período habilitado para reservar online");
        }
    }

    private Confirmada reservarEnTransaccion(SolicitudReserva solicitud, Intervalo intervalo, Instant ahora) {
        bloqueo.bloquear(solicitud.email());
        var fichas = pacientes.activosPorEmail(solicitud.email());
        long activos = turnosActivos.contarFuturosActivos(fichas.stream().map(Paciente::id).toList(), ahora);
        if (activos >= configuracion.maxTurnosActivosPorEmail()) {
            throw limite(LIMITE_TOPE_EMAIL, "Ya tenés el máximo de turnos activos para reservar online");
        }
        var existente = SeleccionFicha.elegir(fichas, solicitud.telefono());
        Long sillon = elegirSillon(solicitud, intervalo);
        var aceptadoEn = ahora.truncatedTo(ChronoUnit.MICROS);
        var ficha = existente.orElseGet(() -> altaPaciente.registrar(new AltaPacienteReservaOnline.DatosFicha(
                solicitud.nombre(), solicitud.apellido(), solicitud.email(), solicitud.telefono(),
                solicitud.obraSocial()), ConsentimientoDatos.VERSION_VIGENTE));
        var turno = agenda.crearOnline(new SolicitudTurno(ficha.id(), solicitud.profesionalId(), sillon,
                solicitud.prestacionId(), solicitud.inicio(), null));
        if (existente.isPresent()) {
            consentimientos.save(new ConsentimientoTurno(turno.id(), aceptadoEn, ConsentimientoDatos.VERSION_VIGENTE));
        }
        var token = tokens.emitir(turno.id());
        return new Confirmada(turno.id(), ficha, intervalo.inicio(), solicitud.profesionalId(), token.claro());
    }

    /**
     * SU-03: el primer sillón activo libre que ofrece la disponibilidad para ese inicio. Si no ofrece ninguno, no
     * se decide acá por qué (puede estar ocupado o ser un horario inválido): se pasa el primer sillón activo y la
     * agenda responde con su regla (RN-04, RN-09 o RN-01). Sin sillones activos el hueco está ocupado.
     */
    private Long elegirSillon(SolicitudReserva solicitud, Intervalo intervalo) {
        return disponibilidad.consultar(solicitud.profesionalId(), solicitud.prestacionId(), intervalo.inicio(),
                        intervalo.fin(), null).stream()
                .filter(horario -> horario.inicio().equals(intervalo.inicio()))
                .map(horario -> horario.sillonId())
                .findFirst()
                .orElseGet(() -> sillones.listarActivos().stream().map(SillonConsulta.Datos::id).sorted()
                        .findFirst().orElseThrow(() -> new ReglaNegocioException("RN-01", EstadoError.CONFLICTO_409,
                                "No hay sillones disponibles")));
    }

    /** RN-08: 409 sin el recurso que chocó, con los próximos horarios libres posteriores al pedido. */
    private ReglaNegocioException ocupadoConAlternativas(SolicitudReserva solicitud) {
        var alternativas = disponibilidadPublica.consultar(solicitud.profesionalId(), solicitud.prestacionId(),
                        solicitud.inicio(), solicitud.inicio().plus(VENTANA_ALTERNATIVAS)).stream()
                .filter(horario -> horario.inicio().isAfter(solicitud.inicio()))
                .limit(MAXIMO_ALTERNATIVAS)
                .map(horario -> new HorarioAlternativo(horario.inicio(), horario.fin()))
                .toList();
        return new ReglaNegocioException("RN-01", EstadoError.CONFLICTO_409,
                "El horario ya no está disponible. Elegí otro horario.", Map.of("alternativas", alternativas));
    }

    private void enviarConfirmacionDespuesDelCommit(Confirmada confirmada, PrestacionResumen prestacion) {
        Runnable enviar = () -> notificaciones.registrarYEnviar(TipoNotificacion.CONFIRMACION_RESERVA,
                confirmada.turnoId(), null, confirmada.ficha().id(), new DatosNotificacion(confirmada.ficha().email(),
                        confirmada.ficha().nombre(), confirmada.inicio(), prestacion.nombre(),
                        nombreProfesional(confirmada.profesionalId()), enlace(confirmada.token())));
        // Si la reserva corre dentro de una transacción externa, el commit real es el de esa transacción.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar.run();
                }
            });
        } else {
            enviar.run();
        }
    }

    String enlace(String token) {
        return urlPublica + "/turno/" + token;
    }

    private String nombreProfesional(Long profesionalId) {
        return profesionales.buscarPorId(profesionalId).map(p -> p.nombre() + " " + p.apellido()).orElse(null);
    }

    private static ReglaNegocioException limite(String limite, String mensaje) {
        return new ReglaNegocioException("RN-27", EstadoError.NO_PROCESABLE_422, mensaje, Map.of("limite", limite));
    }

    /** Lo que se confirma en la transacción; el token en claro no se imprime. */
    private record Confirmada(Long turnoId, Paciente ficha, Instant inicio, Long profesionalId, String token) {

        @Override
        public String toString() {
            return "Confirmada[turnoId=" + turnoId + "]";
        }
    }

    /** Alternativa del 409: solo inicio y fin, sin sillón (RN-35). */
    public record HorarioAlternativo(Instant inicio, Instant fin) {
    }
}
