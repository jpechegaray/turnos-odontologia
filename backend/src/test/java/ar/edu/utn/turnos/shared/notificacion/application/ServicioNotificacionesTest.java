package ar.edu.utn.turnos.shared.notificacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionPort;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.PropiedadesNotificaciones;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Servicio de notificaciones con un repositorio en memoria que respeta las dos restricciones únicas,
 * el adaptador en memoria real y un reloj mutable. La persistencia real se cubre en
 * {@code NotificacionRepositorioIT} y el planificador en {@code PlanificadorNotificacionesIT}.
 */
class ServicioNotificacionesTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");

    private RelojMutable reloj;
    private RepositorioNotificacionesEnMemoria repositorio;
    private InMemoryNotificacionAdapter adaptador;
    private ServicioNotificaciones servicio;

    @BeforeEach
    void preparar() {
        reloj = new RelojMutable(AHORA);
        repositorio = new RepositorioNotificacionesEnMemoria();
        adaptador = new InMemoryNotificacionAdapter();
        ConfiguracionServicio configuracion = mock(ConfiguracionServicio.class);
        when(configuracion.nombreConsultorio()).thenReturn("Consultorio Ficticio");
        servicio = new ServicioNotificaciones(
                repositorio,
                adaptador,
                new PlantillaNotificacion(configuracion),
                propiedades(),
                reloj,
                gestorDeTransaccionesNulo());
    }

    private static PropiedadesNotificaciones propiedades() {
        return new PropiedadesNotificaciones(
                "memoria",
                "turnos@consultorio.test",
                new PropiedadesNotificaciones.Planificador(false, Duration.ofMinutes(5)),
                new PropiedadesNotificaciones.Reintento(3, Duration.ofMinutes(5), 2));
    }

    private static DatosNotificacion datos(String destinatario) {
        return new DatosNotificacion(
                destinatario, "Ana Ficticia", AHORA.plus(Duration.ofHours(24)), "Limpieza", "Dra. Ficticia", "https://t/abc");
    }

    @Test
    @DisplayName("No se reenvía un aviso que ya tiene registro")
    void noSeReenviaUnAvisoQueYaTieneRegistro() {
        DatosNotificacion datos = datos("ana@example.test");

        servicio.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos);
        servicio.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos);

        assertThat(adaptador.enviados()).hasSize(1);
        assertThat(repositorio.todos()).hasSize(1);
    }

    @Test
    @DisplayName("El primer registro de una entrada envía su confirmación una sola vez")
    void confirmaIngresoUnaSolaVez() {
        var ingreso = new DatosNotificacion("luna.demo-lista@example.test", "Luna", null, "Limpieza", null, null);
        servicio.registrarIngresoLista(8L, 5L, ingreso);
        servicio.registrarIngresoLista(8L, 5L, ingreso);

        assertThat(repositorio.todos()).singleElement().satisfies(aviso -> {
            assertThat(aviso.tipo()).isEqualTo(TipoNotificacion.INGRESO_LISTA_ESPERA);
            assertThat(aviso.entradaListaEsperaId()).isEqualTo(8L);
        });
        assertThat(adaptador.enviados()).hasSize(1);
    }

    @Test
    @DisplayName("Un paciente sin email no genera notificación ni error")
    void unPacienteSinEmailNoGeneraNotificacionNiError() {
        DatosNotificacion sinEmail = new DatosNotificacion("   ", "Ana Ficticia", AHORA, null, null, null);

        assertThatCode(() -> servicio.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, sinEmail))
                .doesNotThrowAnyException();

        assertThat(adaptador.enviados()).isEmpty();
        assertThat(repositorio.todos()).isEmpty();
    }

    @Test
    @DisplayName("Un fallo de SMTP deja la notificación FALLIDA y conserva el turno")
    void unFalloDeSmtpDejaLaNotificacionFallidaYConservaElTurno() {
        adaptador.fallarProximos(1);

        servicio.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos("ana@example.test"));

        NotificacionEnviada fallida = repositorio.todos().get(0);
        assertThat(fallida.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(fallida.ultimoError()).isNotBlank();
        assertThat(fallida.turnoId()).isEqualTo(10L);
        assertThat(fallida.intentos()).isEqualTo(1);
        assertThat(fallida.programadaPara()).isEqualTo(AHORA.plus(Duration.ofMinutes(5)));
        assertThat(adaptador.enviados()).isEmpty();
    }

    @Test
    @DisplayName("Una notificación fallida se reintenta hasta tres veces con espera creciente")
    void unaNotificacionFallidaSeReintentaHastaTresVecesConEsperaCreciente() {
        adaptador.fallarProximos(10);

        servicio.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos("ana@example.test"));
        NotificacionEnviada trasPrimero = repositorio.todos().get(0);
        assertThat(trasPrimero.intentos()).isEqualTo(1);
        assertThat(trasPrimero.programadaPara()).isEqualTo(AHORA.plus(Duration.ofMinutes(5)));

        reloj.ajustar(trasPrimero.programadaPara());
        servicio.procesarVencidas();
        NotificacionEnviada trasSegundo = repositorio.porId(trasPrimero.id()).orElseThrow();
        assertThat(trasSegundo.intentos()).isEqualTo(2);
        assertThat(trasSegundo.programadaPara())
                .isEqualTo(AHORA.plus(Duration.ofMinutes(5)).plus(Duration.ofMinutes(10)));

        reloj.ajustar(trasSegundo.programadaPara());
        servicio.procesarVencidas();
        NotificacionEnviada trasTercero = repositorio.porId(trasPrimero.id()).orElseThrow();
        assertThat(trasTercero.intentos()).isEqualTo(3);
        assertThat(trasTercero.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(trasTercero.programadaPara()).isNull();

        reloj.ajustar(AHORA.plus(Duration.ofDays(1)));
        servicio.procesarVencidas();
        assertThat(adaptador.intentos()).isEqualTo(3);
        assertThat(repositorio.porId(trasPrimero.id()).orElseThrow().intentos()).isEqualTo(3);
    }

    @Test
    @DisplayName("El planificador envía las notificaciones pendientes vencidas")
    void elPlanificadorEnviaLasNotificacionesPendientesVencidas() {
        NotificacionEnviada pendiente = repositorio.guardar(NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO,
                "ana@example.test",
                10L,
                null,
                5L,
                AHORA.plus(Duration.ofHours(1)),
                AHORA.minusSeconds(1)));

        servicio.procesarVencidas();

        NotificacionEnviada enviada = repositorio.porId(pendiente.id()).orElseThrow();
        assertThat(enviada.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(enviada.enviadaEn()).isEqualTo(AHORA);
        assertThat(adaptador.enviados()).hasSize(1);
    }

    @Test
    @DisplayName("El planificador no envía notificaciones programadas a futuro")
    void elPlanificadorNoEnviaNotificacionesProgramadasAFuturo() {
        NotificacionEnviada futura = repositorio.guardar(NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO,
                "ana@example.test",
                10L,
                null,
                5L,
                AHORA.plus(Duration.ofHours(1)),
                AHORA.plusSeconds(1)));

        servicio.procesarVencidas();

        NotificacionEnviada sinCambios = repositorio.porId(futura.id()).orElseThrow();
        assertThat(sinCambios.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(adaptador.enviados()).isEmpty();
    }

    @Test
    @DisplayName("Los reintentos de un aviso de oferta cesan al comenzar el hueco")
    void losReintentosDeUnAvisoDeOfertaCesanAlComenzarElHueco() {
        Instant inicioHueco = AHORA.plus(Duration.ofMinutes(10));
        NotificacionEnviada aviso = repositorio.guardar(NotificacionEnviada.pendiente(
                        TipoNotificacion.OFERTA_HUECO,
                        "ana@example.test",
                        null,
                        200L,
                        5L,
                        inicioHueco,
                        AHORA.minusSeconds(60))
                .conIntentoFallido("error previo", AHORA.minusSeconds(60)));

        reloj.ajustar(inicioHueco.minusSeconds(1));
        adaptador.fallarProximos(1);
        servicio.procesarVencidas();
        assertThat(repositorio.porId(aviso.id()).orElseThrow().intentos()).isEqualTo(2);
        int intentosAntesDelCorte = adaptador.intentos();

        reloj.ajustar(inicioHueco.plus(Duration.ofMinutes(11)));
        servicio.procesarVencidas();
        assertThat(repositorio.porId(aviso.id()).orElseThrow().intentos()).isEqualTo(2);
        assertThat(adaptador.intentos()).isEqualTo(intentosAntesDelCorte);
    }

    @Test
    @DisplayName("Un envío en curso no se duplica con el planificador")
    void unEnvioEnCursoNoSeDuplicaConElPlanificador() {
        // El planificador corre mientras el envío inmediato todavía no terminó: la fila recién registrada
        // no debe estar a su alcance, o el paciente recibe el mismo email dos veces.
        List<MensajeNotificacion> enviados = new java.util.ArrayList<>();
        ServicioNotificaciones[] enCurso = new ServicioNotificaciones[1];
        AtomicBoolean planificadorDisparado = new AtomicBoolean();
        NotificacionPort puerto = mensaje -> {
            if (planificadorDisparado.compareAndSet(false, true)) {
                enCurso[0].procesarVencidas();
            }
            enviados.add(mensaje);
        };
        enCurso[0] = servicioCon(puerto, List.of());

        enCurso[0].registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos("ana@example.test"));

        assertThat(enviados).hasSize(1);
        NotificacionEnviada fila = repositorio.todos().get(0);
        assertThat(fila.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(fila.intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un reintento vuelve a armar el mensaje con los datos del consumidor")
    void unReintentoVuelveAArmarElMensajeConLosDatosDelConsumidor() {
        ReconstructorDatosNotificacion consumidor = notificacion -> notificacion.turnoId() != null
                && notificacion.turnoId() == 10L
                ? Optional.of(datos(notificacion.destinatario()))
                : Optional.empty();
        ServicioNotificaciones conConsumidor = servicioCon(adaptador, List.of(consumidor));
        adaptador.fallarProximos(1);

        conConsumidor.registrarYEnviar(TipoNotificacion.RECORDATORIO, 10L, null, 5L, datos("ana@example.test"));
        reloj.ajustar(repositorio.todos().get(0).programadaPara());
        conConsumidor.procesarVencidas();

        assertThat(adaptador.enviados()).singleElement().satisfies(mensaje -> assertThat(mensaje.cuerpo())
                .contains("Hola Ana Ficticia", "Prestación: Limpieza", "Profesional: Dra. Ficticia", "Enlace: https://t/abc"));
    }

    private ServicioNotificaciones servicioCon(
            NotificacionPort puerto, List<ReconstructorDatosNotificacion> reconstructores) {
        ConfiguracionServicio configuracion = mock(ConfiguracionServicio.class);
        when(configuracion.nombreConsultorio()).thenReturn("Consultorio Ficticio");
        return new ServicioNotificaciones(
                repositorio,
                puerto,
                new PlantillaNotificacion(configuracion),
                propiedades(),
                reloj,
                gestorDeTransaccionesNulo(),
                reconstructores);
    }

    // ---------- Dobles de prueba ----------

    private static PlatformTransactionManager gestorDeTransaccionesNulo() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }

    /** Reloj mutable para probar la espera creciente y los vencimientos sin tiempos reales. */
    static final class RelojMutable extends Clock {

        private Instant instante;

        RelojMutable(Instant inicial) {
            this.instante = inicial;
        }

        void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        void ajustar(Instant nuevoInstante) {
            instante = nuevoInstante;
        }

        @Override
        public ZoneId getZone() {
            return ZonaHoraria.BUENOS_AIRES;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    /** Repositorio en memoria que aplica las mismas dos restricciones únicas que PostgreSQL. */
    static final class RepositorioNotificacionesEnMemoria implements NotificacionRepositorio {

        private final Map<Long, NotificacionEnviada> filas = new LinkedHashMap<>();
        private long secuencia;

        @Override
        public NotificacionEnviada guardar(NotificacionEnviada notificacion) {
            boolean duplicadoTurno = notificacion.turnoId() != null && filas.values().stream().anyMatch(fila ->
                    !Objects.equals(fila.id(), notificacion.id())
                            && Objects.equals(fila.turnoId(), notificacion.turnoId())
                            && fila.tipo() == notificacion.tipo()
                            && Objects.equals(fila.inicioReferencia(), notificacion.inicioReferencia()));
            boolean duplicadoOferta = notificacion.ofertaHuecoId() != null && filas.values().stream().anyMatch(fila ->
                    !Objects.equals(fila.id(), notificacion.id())
                            && Objects.equals(fila.ofertaHuecoId(), notificacion.ofertaHuecoId())
                            && fila.tipo() == notificacion.tipo());
            boolean duplicadaEntrada = notificacion.entradaListaEsperaId() != null && filas.values().stream().anyMatch(fila ->
                    !Objects.equals(fila.id(), notificacion.id())
                            && Objects.equals(fila.entradaListaEsperaId(), notificacion.entradaListaEsperaId())
                            && fila.tipo() == notificacion.tipo());
            if (duplicadoTurno || duplicadoOferta || duplicadaEntrada) {
                throw new DataIntegrityViolationException("Registro de notificación duplicado");
            }
            Long id = notificacion.id() != null ? notificacion.id() : ++secuencia;
            NotificacionEnviada conId = new NotificacionEnviada(
                    id,
                    notificacion.tipo(),
                    notificacion.canal(),
                    notificacion.destinatario(),
                    notificacion.turnoId(),
                    notificacion.ofertaHuecoId(),
                    notificacion.pacienteId(),
                    notificacion.inicioReferencia(),
                    notificacion.estado(),
                    notificacion.intentos(),
                    notificacion.ultimoError(),
                    notificacion.programadaPara(),
                    notificacion.enviadaEn(),
                    notificacion.entradaListaEsperaId());
            filas.put(id, conId);
            return conId;
        }

        @Override
        public Optional<NotificacionEnviada> porId(Long id) {
            return Optional.ofNullable(filas.get(id));
        }

        @Override
        public List<NotificacionEnviada> reclamarVencidas(Instant ahora, int maximoIntentos) {
            return filas.values().stream()
                    .filter(fila -> fila.estado() != EstadoNotificacion.ENVIADA)
                    .filter(fila -> fila.programadaPara() != null && !fila.programadaPara().isAfter(ahora))
                    .filter(fila -> fila.estado() == EstadoNotificacion.PENDIENTE || fila.intentos() < maximoIntentos)
                    .filter(fila -> !(fila.tipo() == TipoNotificacion.OFERTA_HUECO
                            && fila.inicioReferencia() != null
                            && !fila.inicioReferencia().isAfter(ahora)))
                    .toList();
        }

        /** La consulta del registro (C-25) no la usa el servicio de envío; se prueba en NotificacionRepositorioIT. */
        @Override
        public ar.edu.utn.turnos.shared.paginacion.Pagina<NotificacionEnviada> buscar(
                EstadoNotificacion estado, Long turnoId, int pagina, int tamanio) {
            throw new UnsupportedOperationException("ServicioNotificaciones no consulta el registro");
        }

        List<NotificacionEnviada> todos() {
            return List.copyOf(filas.values());
        }
    }
}
