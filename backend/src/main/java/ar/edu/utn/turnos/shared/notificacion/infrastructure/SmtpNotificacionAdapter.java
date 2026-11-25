package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import ar.edu.utn.turnos.shared.notificacion.domain.ErrorEnvioNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionPort;
import java.util.Objects;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Adaptador SMTP sobre {@link JavaMailSender}. Se activa con {@code app.notificaciones.canal=smtp}.
 *
 * <p>Valida al construirse que host, usuario y contraseña no estén en blanco: es el fail-fast del
 * arranque. El mensaje de error nombra la variable faltante pero <b>nunca</b> imprime su valor. Al
 * enviar no se loguea el cuerpo ni las credenciales; un error del proveedor se traduce en
 * {@link ErrorEnvioNotificacion} para que el servicio lo registre y reintente.
 */
public class SmtpNotificacionAdapter implements NotificacionPort {

    private final JavaMailSender javaMailSender;
    private final String remitente;

    public SmtpNotificacionAdapter(
            JavaMailSender javaMailSender,
            PropiedadesNotificaciones propiedades,
            String host,
            String usuario,
            String password) {
        this.javaMailSender = Objects.requireNonNull(javaMailSender, "javaMailSender");
        this.remitente = propiedades.remitente();
        exigirCredencial(host, "SMTP_HOST");
        exigirCredencial(usuario, "SMTP_USER");
        exigirCredencial(password, "SMTP_PASSWORD");
    }

    @Override
    public void enviar(MensajeNotificacion mensaje) {
        try {
            SimpleMailMessage correo = new SimpleMailMessage();
            correo.setFrom(remitente);
            correo.setTo(mensaje.destinatario());
            correo.setSubject(mensaje.asunto());
            correo.setText(mensaje.cuerpo());
            javaMailSender.send(correo);
        } catch (MailException e) {
            throw new ErrorEnvioNotificacion("No se pudo enviar la notificación por email", e);
        }
    }

    private static void exigirCredencial(String valor, String variable) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno obligatoria " + variable + " para el canal SMTP");
        }
    }
}
