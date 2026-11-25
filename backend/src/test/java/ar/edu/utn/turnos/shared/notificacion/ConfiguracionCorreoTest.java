package ar.edu.utn.turnos.shared.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Contrato de configuración de correo por entorno: los valores comunes salen de variables de
 * entorno y el perfil de pruebas usa el canal en memoria con el planificador apagado.
 */
class ConfiguracionCorreoTest {

    private static PropertySource<?> cargar(String archivo) throws IOException {
        List<PropertySource<?>> fuentes = new YamlPropertySourceLoader()
                .load(archivo, new ClassPathResource(archivo + ".yml"));
        return fuentes.get(0);
    }

    @Test
    @DisplayName("Las credenciales SMTP se toman solo del entorno")
    void lasCredencialesSmtpSeTomanSoloDelEntorno() throws IOException {
        PropertySource<?> comun = cargar("application");

        assertThat(comun.getProperty("spring.mail.host")).isEqualTo("${SMTP_HOST:}");
        assertThat(comun.getProperty("spring.mail.username")).isEqualTo("${SMTP_USER:}");
        assertThat(comun.getProperty("spring.mail.password")).isEqualTo("${SMTP_PASSWORD:}");
        assertThat(comun.getProperty("spring.mail.port")).isEqualTo("${SMTP_PORT:587}");
        assertThat(comun.getProperty("app.notificaciones.remitente"))
                .isEqualTo("${SMTP_FROM:turnos@consultorio.test}");
        assertThat(comun.getProperty("app.notificaciones.canal"))
                .isEqualTo("${NOTIFICACIONES_CANAL:smtp}");
    }

    @Test
    @DisplayName("El bloque de notificaciones configura planificador y reintentos")
    void elBloqueDeNotificacionesConfiguraPlanificadorYReintentos() throws IOException {
        PropertySource<?> comun = cargar("application");

        assertThat(comun.getProperty("app.notificaciones.planificador.habilitado")).isEqualTo(true);
        assertThat(comun.getProperty("app.notificaciones.planificador.intervalo")).isEqualTo("5m");
        assertThat(comun.getProperty("app.notificaciones.reintento.maximo-intentos")).isEqualTo(3);
        assertThat(comun.getProperty("app.notificaciones.reintento.espera-inicial")).isEqualTo("5m");
        assertThat(comun.getProperty("app.notificaciones.reintento.multiplicador")).isEqualTo(2);
    }

    @Test
    @DisplayName("Las pruebas usan el adaptador en memoria")
    void lasPruebasUsanElAdaptadorEnMemoria() throws IOException {
        PropertySource<?> prueba = cargar("application-test");

        assertThat(prueba.getProperty("app.notificaciones.canal")).isEqualTo("memoria");
        assertThat(prueba.getProperty("app.notificaciones.planificador.habilitado")).isEqualTo(false);
    }

    @Test
    @DisplayName("El repositorio no contiene credenciales SMTP")
    void elRepositorioNoContieneCredencialesSmtp() throws IOException {
        String ejemplo = Files.readString(Path.of("..", ".env.example"));

        assertThat(ejemplo).contains("NOTIFICACIONES_CANAL=smtp");
        assertThat(ejemplo).contains("SMTP_HOST=");
        assertThat(ejemplo).contains("SMTP_PASSWORD=cambiar-en-env-local");
        assertThat(ejemplo).doesNotContain("SMTP_PASSWORD=changeme");
    }
}
