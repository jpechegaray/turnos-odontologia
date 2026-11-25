package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración de notificaciones: habilita el planificador y elige el adaptador del puerto por
 * propiedad ({@code @ConditionalOnProperty}, no por perfil, para cambiar de canal sin recompilar).
 *
 * <p>El adaptador SMTP es el default ({@code matchIfMissing=true}); el perfil de pruebas fija
 * {@code app.notificaciones.canal=memoria}. {@code @EnableScheduling} vive acá, no en la clase raíz.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesNotificaciones.class)
public class ConfiguracionNotificaciones {

    @Bean
    @ConditionalOnProperty(name = "app.notificaciones.canal", havingValue = "memoria")
    public InMemoryNotificacionAdapter inMemoryNotificacionAdapter() {
        return new InMemoryNotificacionAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notificaciones.canal", havingValue = "smtp", matchIfMissing = true)
    public SmtpNotificacionAdapter smtpNotificacionAdapter(
            JavaMailSender javaMailSender,
            PropiedadesNotificaciones propiedades,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.username:}") String usuario,
            @Value("${spring.mail.password:}") String password) {
        return new SmtpNotificacionAdapter(javaMailSender, propiedades, host, usuario, password);
    }
}
