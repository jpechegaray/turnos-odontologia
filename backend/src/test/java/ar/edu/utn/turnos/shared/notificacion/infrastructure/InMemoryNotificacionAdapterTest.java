package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.notificacion.domain.ErrorEnvioNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Adaptador en memoria: registra envíos, cuenta intentos y permite forzar fallos para los tests de
 * reintento. Nunca abre una conexión SMTP.
 */
class InMemoryNotificacionAdapterTest {

    private static final MensajeNotificacion MENSAJE =
            new MensajeNotificacion("paciente@example.test", "Asunto", "Cuerpo");

    private final InMemoryNotificacionAdapter adaptador = new InMemoryNotificacionAdapter();

    @Test
    @DisplayName("Las pruebas usan el adaptador en memoria")
    void registraElEnvioSinAbrirSmtp() {
        adaptador.enviar(MENSAJE);

        assertThat(adaptador.enviados()).containsExactly(MENSAJE);
        assertThat(adaptador.intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("El adaptador en memoria puede forzar fallos para probar reintentos")
    void puedeForzarFallosParaProbarReintentos() {
        adaptador.fallarProximos(2);

        assertThatThrownBy(() -> adaptador.enviar(MENSAJE)).isInstanceOf(ErrorEnvioNotificacion.class);
        assertThatThrownBy(() -> adaptador.enviar(MENSAJE)).isInstanceOf(ErrorEnvioNotificacion.class);
        adaptador.enviar(MENSAJE);

        assertThat(adaptador.enviados()).containsExactly(MENSAJE);
        assertThat(adaptador.intentos()).isEqualTo(3);
    }

    @Test
    @DisplayName("Limpiar el adaptador en memoria descarta envíos y fallos pendientes")
    void limpiarDescartaEnviosYFallosPendientes() {
        adaptador.fallarProximos(1);
        assertThatThrownBy(() -> adaptador.enviar(MENSAJE)).isInstanceOf(ErrorEnvioNotificacion.class);

        adaptador.limpiar();
        adaptador.enviar(MENSAJE);

        assertThat(adaptador.enviados()).containsExactly(MENSAJE);
        assertThat(adaptador.intentos()).isEqualTo(1);
    }
}
