package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.notificacion.application.PlantillaNotificacion;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Bordes del planificador contra PostgreSQL real con Testcontainers: reintento de fallidas vencidas,
 * exclusión mutua de dos ejecuciones concurrentes y reanudación de pendientes tras un reinicio. Usa el
 * adaptador en memoria (perfil {@code test}) y un reloj de sistema; la exclusión la da
 * {@code FOR UPDATE SKIP LOCKED} en {@link NotificacionRepositorioAdapter}.
 */
class PlanificadorNotificacionesIT extends PruebaIntegracionBase {

    @Autowired
    private ServicioNotificaciones servicio;

    @Autowired
    private NotificacionRepositorio repositorio;

    @Autowired
    private NotificacionEnviadaJpaRepository jpaRepository;

    @Autowired
    private InMemoryNotificacionAdapter adaptador;

    @Autowired
    private PlantillaNotificacion plantilla;

    @Autowired
    private PropiedadesNotificaciones propiedades;

    @Autowired
    private Clock reloj;

    @Autowired
    private PlatformTransactionManager gestorTransacciones;

    @BeforeEach
    void limpiar() {
        jpaRepository.deleteAll();
        adaptador.limpiar();
    }

    private NotificacionEnviada fallidaDeTurno(Instant ahora) {
        Instant programada = ahora.minus(Duration.ofMinutes(1));
        return repositorio.guardar(NotificacionEnviada.pendiente(
                        TipoNotificacion.RECORDATORIO,
                        "ana@example.test",
                        10L,
                        null,
                        5L,
                        ahora.plus(Duration.ofHours(1)),
                        programada)
                .conIntentoFallido("error previo", programada));
    }

    @Test
    @DisplayName("El planificador reintenta las fallidas cuyo próximo intento venció")
    void elPlanificadorReintentaLasFallidasCuyoProximoIntentoVencio() {
        NotificacionEnviada fallida = fallidaDeTurno(reloj.instant());

        servicio.procesarVencidas();

        NotificacionEnviada procesada = repositorio.porId(fallida.id()).orElseThrow();
        assertThat(procesada.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(procesada.intentos()).isEqualTo(2);
        assertThat(adaptador.enviados()).hasSize(1);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Dos ejecuciones concurrentes del planificador no duplican un envío")
    void dosEjecucionesConcurrentesDelPlanificadorNoDuplicanUnEnvio() throws Exception {
        Instant ahora = reloj.instant();
        repositorio.guardar(NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO,
                "ana@example.test",
                10L,
                null,
                5L,
                ahora.plus(Duration.ofHours(1)),
                ahora.minusSeconds(60)));

        CountDownLatch envioIniciado = new CountDownLatch(1);
        CountDownLatch continuarEnvio = new CountDownLatch(1);
        adaptador.bloquearProximoEnvio(envioIniciado, continuarEnvio);

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<?> primera = ejecutor.submit(servicio::procesarVencidas);
            assertThat(envioIniciado.await(10, TimeUnit.SECONDS))
                    .as("La primera ejecución debe reclamar la fila y quedar bloqueada en el envío")
                    .isTrue();

            Future<?> segunda = ejecutor.submit(servicio::procesarVencidas);
            segunda.get(10, TimeUnit.SECONDS);

            continuarEnvio.countDown();
            primera.get(10, TimeUnit.SECONDS);
        } finally {
            ejecutor.shutdownNow();
        }

        assertThat(adaptador.intentos()).isEqualTo(1);
        assertThat(adaptador.enviados()).hasSize(1);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un reinicio del servicio retoma las pendientes en el siguiente ciclo")
    void unReinicioDelServicioRetomaLasPendientesEnElSiguienteCiclo() {
        Instant ahora = reloj.instant();
        NotificacionEnviada pendiente = repositorio.guardar(NotificacionEnviada.pendiente(
                TipoNotificacion.CONFIRMACION_RESERVA,
                "ana@example.test",
                20L,
                null,
                6L,
                ahora.plus(Duration.ofHours(1)),
                ahora.minusSeconds(60)));

        ServicioNotificaciones reiniciado = new ServicioNotificaciones(
                repositorio, adaptador, plantilla, propiedades, reloj, gestorTransacciones);
        reiniciado.procesarVencidas();

        NotificacionEnviada enviada = repositorio.porId(pendiente.id()).orElseThrow();
        assertThat(enviada.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(adaptador.enviados()).hasSize(1);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
