package ar.edu.utn.turnos.shared.notificacion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Valores del contrato de notificaciones. La KB 04 fija exactamente los tres estados y los cinco tipos.
 */
class EnumsNotificacionTest {

    @Test
    @DisplayName("El estado de notificación tiene exactamente PENDIENTE, ENVIADA y FALLIDA")
    void estadoTieneLosTresValoresDelModelo() {
        assertThat(EstadoNotificacion.values())
                .containsExactly(
                        EstadoNotificacion.PENDIENTE,
                        EstadoNotificacion.ENVIADA,
                        EstadoNotificacion.FALLIDA);
    }

    @Test
    @DisplayName("El tipo de notificación tiene exactamente los cinco valores del modelo")
    void tipoTieneLosCincoValoresDelModelo() {
        assertThat(TipoNotificacion.values())
                .containsExactly(
                        TipoNotificacion.RECORDATORIO,
                        TipoNotificacion.CONFIRMACION_RESERVA,
                        TipoNotificacion.CANCELACION,
                        TipoNotificacion.REPROGRAMACION,
                        TipoNotificacion.OFERTA_HUECO);
    }
}
