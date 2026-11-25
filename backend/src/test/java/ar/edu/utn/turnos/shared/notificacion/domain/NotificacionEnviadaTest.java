package ar.edu.utn.turnos.shared.notificacion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Transiciones del registro inmutable de notificación. El reloj es un {@link Instant} fijo que
 * pasa el servicio, de modo que la espera creciente y la vigencia son deterministas.
 */
class NotificacionEnviadaTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");
    private static final Duration ESPERA_INICIAL = Duration.ofMinutes(5);

    private static NotificacionEnviada pendiente() {
        return NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO,
                "paciente@example.test",
                10L,
                null,
                5L,
                AHORA.plus(Duration.ofHours(1)),
                AHORA);
    }

    @Test
    @DisplayName("Un envío exitoso queda ENVIADA con la fecha de envío")
    void envioExitosoQuedaEnviadaConFechaDeEnvio() {
        NotificacionEnviada enviada = pendiente().conEnvioExitoso(AHORA);

        assertThat(enviada.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(enviada.enviadaEn()).isEqualTo(AHORA);
        assertThat(enviada.ultimoError()).isNull();
        assertThat(enviada.intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("El próximo intento se calcula con el reloj inyectable")
    void proximoIntentoSeCalculaConElRelojInyectable() {
        assertThat(NotificacionEnviada.esperaCreciente(1, ESPERA_INICIAL, 2))
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(NotificacionEnviada.esperaCreciente(2, ESPERA_INICIAL, 2))
                .isEqualTo(Duration.ofMinutes(10));

        Instant proximo = AHORA.plus(NotificacionEnviada.esperaCreciente(1, ESPERA_INICIAL, 2));
        NotificacionEnviada fallida = pendiente().conIntentoFallido("SMTP no disponible", proximo);

        assertThat(fallida.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(fallida.programadaPara()).isEqualTo(AHORA.plus(ESPERA_INICIAL));
        assertThat(fallida.ultimoError()).isEqualTo("SMTP no disponible");
        assertThat(fallida.intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("La espera entre reintentos es estrictamente creciente")
    void laEsperaEntreReintentosEsEstrictamenteCreciente() {
        Duration primerIntento = NotificacionEnviada.esperaCreciente(1, ESPERA_INICIAL, 2);
        Duration segundoIntento = NotificacionEnviada.esperaCreciente(2, ESPERA_INICIAL, 2);
        Duration tercerIntento = NotificacionEnviada.esperaCreciente(3, ESPERA_INICIAL, 2);

        assertThat(segundoIntento).isGreaterThan(primerIntento);
        assertThat(tercerIntento).isGreaterThan(segundoIntento);
    }

    @Test
    @DisplayName("Una notificación fallida no se reintenta después del tercer intento")
    void notificacionFallidaNoSeReintentaDespuesDelTercerIntento() {
        NotificacionEnviada fallida = pendiente()
                .conIntentoFallido("error 1", AHORA.plus(Duration.ofMinutes(5)))
                .conIntentoFallido("error 2", AHORA.plus(Duration.ofMinutes(15)))
                .conIntentoFallido("error 3", null);

        assertThat(fallida.estado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(fallida.intentos()).isEqualTo(3);
        assertThat(fallida.admiteReintento(3)).isFalse();
        assertThat(fallida.vencida(AHORA.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    @DisplayName("Una notificación pendiente está vencida solo cuando su programación ya pasó")
    void pendienteVenceCuandoSuProgramacionYaPaso() {
        assertThat(pendiente().vencida(AHORA.plusSeconds(1))).isTrue();
        assertThat(pendiente().vencida(AHORA.minusSeconds(1))).isFalse();
        assertThat(pendiente().conEnvioExitoso(AHORA).vencida(AHORA.plus(Duration.ofDays(1)))).isFalse();
    }
}
