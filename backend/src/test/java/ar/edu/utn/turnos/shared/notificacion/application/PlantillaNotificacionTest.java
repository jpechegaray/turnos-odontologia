package ar.edu.utn.turnos.shared.notificacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La plantilla arma el email de un solo destinatario con el nombre vigente del consultorio, la fecha
 * en Buenos Aires y sin datos de otros pacientes (RN-31, RN-35).
 */
class PlantillaNotificacionTest {

    private final ConfiguracionServicio configuracion = mock(ConfiguracionServicio.class);
    private final PlantillaNotificacion plantilla = new PlantillaNotificacion(configuracion);

    @BeforeEach
    void configurarConsultorio() {
        when(configuracion.nombreConsultorio()).thenReturn("Consultorio Ficticio");
    }

    @Test
    @DisplayName("La notificación usa el nombre del consultorio y no expone otros pacientes")
    void usaElConsultorioYNoExponeOtrosPacientes() {
        DatosNotificacion pacienteA = new DatosNotificacion(
                "ana@example.test",
                "Ana Ficticia",
                Instant.parse("2026-12-10T14:30:00Z"),
                "Limpieza",
                "Dra. Profesional Ficticia",
                "https://turnos.test/t/abc");

        MensajeNotificacion mensaje = plantilla.armar(TipoNotificacion.RECORDATORIO, pacienteA);

        assertThat(mensaje.destinatario()).isEqualTo("ana@example.test");
        assertThat(mensaje.asunto()).contains("Consultorio Ficticio");
        assertThat(mensaje.cuerpo())
                .contains("Consultorio Ficticio")
                .contains("Ana Ficticia")
                .doesNotContain("Bruno Inventado")
                .doesNotContain("bruno@example.test");
    }

    @Test
    @DisplayName("La plantilla formatea la fecha en la zona de Buenos Aires")
    void formateaLaFechaEnBuenosAires() {
        DatosNotificacion datos = new DatosNotificacion(
                "ana@example.test", "Ana Ficticia", Instant.parse("2026-12-10T14:30:00Z"), null, null, null);

        MensajeNotificacion mensaje = plantilla.armar(TipoNotificacion.RECORDATORIO, datos);

        // 14:30 UTC son las 11:30 en America/Argentina/Buenos_Aires.
        assertThat(mensaje.cuerpo()).contains("10/12/2026 11:30");
    }

    @Test
    @DisplayName("La plantilla omite los datos opcionales ausentes")
    void omiteLosDatosOpcionalesAusentes() {
        DatosNotificacion datos = new DatosNotificacion("ana@example.test", null, null, null, null, null);

        String cuerpo = plantilla.armar(TipoNotificacion.OFERTA_HUECO, datos).cuerpo();

        assertThat(cuerpo)
                .contains("Consultorio Ficticio")
                .doesNotContain("Prestación:")
                .doesNotContain("Profesional:")
                .doesNotContain("Enlace:");
    }
}
