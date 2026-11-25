package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ar.edu.utn.turnos.shared.notificacion.domain.ErrorEnvioNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Adaptador SMTP: valida credenciales al construirse (fail-fast) y entrega el mensaje con el
 * remitente configurado. Nunca revela el valor de una credencial en el error.
 */
class SmtpNotificacionAdapterTest {

    private static final MensajeNotificacion MENSAJE =
            new MensajeNotificacion("paciente@example.test", "Asunto", "Cuerpo");

    private static PropiedadesNotificaciones propiedades() {
        return new PropiedadesNotificaciones(
                "smtp",
                "turnos@consultorio.test",
                new PropiedadesNotificaciones.Planificador(true, Duration.ofMinutes(5)),
                new PropiedadesNotificaciones.Reintento(3, Duration.ofMinutes(5), 2));
    }

    @Test
    @DisplayName("Las credenciales SMTP se toman solo del entorno")
    void usaElRemitenteConfiguradoYElDestinatario() {
        JavaMailSender sender = mock(JavaMailSender.class);
        SmtpNotificacionAdapter adaptador = new SmtpNotificacionAdapter(
                sender, propiedades(), "smtp.ejemplo.test", "usuario-smtp", "secreto-en-entorno");

        adaptador.enviar(MENSAJE);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("turnos@consultorio.test");
        assertThat(captor.getValue().getTo()).containsExactly("paciente@example.test");
        assertThat(captor.getValue().getSubject()).isEqualTo("Asunto");
        assertThat(captor.getValue().getText()).isEqualTo("Cuerpo");
    }

    @Test
    @DisplayName("El arranque falla si falta una credencial SMTP obligatoria")
    void fallaSiFaltaLaPasswordSinRevelarElValor() {
        JavaMailSender sender = mock(JavaMailSender.class);

        assertThatThrownBy(() -> new SmtpNotificacionAdapter(
                sender, propiedades(), "smtp.ejemplo.test", "usuario-smtp", "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP_PASSWORD");
    }

    @Test
    @DisplayName("El arranque falla si falta el host o el usuario sin revelar credenciales")
    void fallaSiFaltaElHostOElUsuario() {
        JavaMailSender sender = mock(JavaMailSender.class);

        assertThatThrownBy(() -> new SmtpNotificacionAdapter(
                sender, propiedades(), "  ", "usuario-smtp", "secreto-super"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP_HOST")
                .hasMessageNotContaining("secreto-super");

        assertThatThrownBy(() -> new SmtpNotificacionAdapter(
                sender, propiedades(), "smtp.ejemplo.test", "", "secreto-super"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP_USER")
                .hasMessageNotContaining("secreto-super");
    }

    @Test
    @DisplayName("Un error del servidor de correo se traduce en ErrorEnvioNotificacion")
    void unErrorDelServidorSeTraduceEnErrorDeEnvio() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caído")).when(sender).send(any(SimpleMailMessage.class));
        SmtpNotificacionAdapter adaptador = new SmtpNotificacionAdapter(
                sender, propiedades(), "smtp.ejemplo.test", "usuario-smtp", "secreto-en-entorno");

        assertThatThrownBy(() -> adaptador.enviar(MENSAJE))
                .isInstanceOf(ErrorEnvioNotificacion.class)
                .hasMessageNotContaining("Cuerpo");
    }
}
