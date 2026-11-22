package ar.edu.utn.turnos.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LimitadorTasaEnMemoriaTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");

    @Test
    @DisplayName("Al reiniciar la ventana la misma IP vuelve a recibir respuestas normales")
    void alReiniciarLaVentanaLaMismaIpVuelveARecibirRespuestasNormales() {
        var reloj = new RelojMutable(T0);
        var limitador = new LimitadorTasaEnMemoria(2, Duration.ofSeconds(60), reloj);

        assertThat(limitador.evaluar("203.0.113.10").permitido()).isTrue();
        assertThat(limitador.evaluar("203.0.113.10").permitido()).isTrue();

        var rechazado = limitador.evaluar("203.0.113.10");
        assertThat(rechazado.permitido()).isFalse();
        assertThat(rechazado.segundosReintento()).isEqualTo(60L);

        reloj.avanzar(Duration.ofMillis(59_500));
        var rechazadoFaltandoMedioSegundo = limitador.evaluar("203.0.113.10");
        assertThat(rechazadoFaltandoMedioSegundo.permitido()).isFalse();
        assertThat(rechazadoFaltandoMedioSegundo.segundosReintento()).isEqualTo(1L);

        reloj.avanzar(Duration.ofMillis(500));
        var trasReinicio = limitador.evaluar("203.0.113.10");
        assertThat(trasReinicio.permitido()).isTrue();
        assertThat(trasReinicio.segundosReintento()).isZero();
    }

    @Test
    @DisplayName("Los contadores de ventanas vencidas se limpian de la memoria")
    void losContadoresDeVentanasVencidasSeLimpianDeLaMemoria() {
        var reloj = new RelojMutable(T0);
        var limitador = new LimitadorTasaEnMemoria(3, Duration.ofSeconds(30), reloj);

        limitador.evaluar("198.51.100.1");
        limitador.evaluar("198.51.100.2");
        assertThat(limitador.cantidadContadoresActivos()).isEqualTo(2);

        reloj.avanzar(Duration.ofSeconds(10));
        limitador.evaluar("198.51.100.3");
        assertThat(limitador.cantidadContadoresActivos()).isEqualTo(3);

        reloj.avanzar(Duration.ofSeconds(20));
        limitador.evaluar("198.51.100.4");

        assertThat(limitador.cantidadContadoresActivos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Respeta el tope exacto ante evaluaciones concurrentes de la misma clave")
    void respetaTopeExactoEnConcurrencia() throws InterruptedException {
        var reloj = new RelojMutable(T0);
        var limitador = new LimitadorTasaEnMemoria(5, Duration.ofSeconds(60), reloj);
        int hilos = 20;
        var listos = new CountDownLatch(hilos);
        var largada = new CountDownLatch(1);
        var terminados = new CountDownLatch(hilos);
        var permitidos = new AtomicInteger();
        var rechazados = new AtomicInteger();

        var pool = Executors.newFixedThreadPool(hilos);
        try {
            for (int i = 0; i < hilos; i++) {
                pool.submit(() -> {
                    listos.countDown();
                    try {
                        largada.await();
                        if (limitador.evaluar("203.0.113.50").permitido()) {
                            permitidos.incrementAndGet();
                        } else {
                            rechazados.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        terminados.countDown();
                    }
                });
            }
            listos.await(5, TimeUnit.SECONDS);
            largada.countDown();
            assertThat(terminados.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(permitidos.get()).isEqualTo(5);
        assertThat(rechazados.get()).isEqualTo(15);
    }

    static final class RelojMutable extends Clock {
        private Instant actual;

        RelojMutable(Instant inicial) {
            this.actual = inicial;
        }

        void avanzar(Duration delta) {
            this.actual = this.actual.plus(delta);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return actual;
        }
    }
}
