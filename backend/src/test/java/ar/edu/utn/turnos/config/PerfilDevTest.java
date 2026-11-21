package ar.edu.utn.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class PerfilDevTest {

    private static PropertySource<?> cargarPerfilDev() throws IOException {
        List<PropertySource<?>> fuentes = new YamlPropertySourceLoader()
                .load("application-dev", new ClassPathResource("application-dev.yml"));
        return fuentes.get(0);
    }

    @Test
    @DisplayName("El perfil dev conserva el esquema entre arranques")
    void perfilDevConservaElEsquemaEntreArranques() throws IOException {
        assertThat(cargarPerfilDev().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
    }

    @Test
    @DisplayName("El perfil dev lee las credenciales de variables de entorno y no las escribe en el código")
    void perfilDevLeeCredencialesDelEntorno() throws IOException {
        PropertySource<?> dev = cargarPerfilDev();

        assertThat(dev.getProperty("spring.datasource.url")).isEqualTo("${SPRING_DATASOURCE_URL}");
        assertThat(dev.getProperty("spring.datasource.username")).isEqualTo("${POSTGRES_USER}");
        assertThat(dev.getProperty("spring.datasource.password")).isEqualTo("${POSTGRES_PASSWORD}");
    }

    @Test
    @DisplayName("El perfil dev enciende springdoc")
    void perfilDevEnciendeSpringdoc() throws IOException {
        PropertySource<?> dev = cargarPerfilDev();

        assertThat(dev.getProperty("springdoc.api-docs.enabled")).isEqualTo(true);
        assertThat(dev.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(true);
    }
}
