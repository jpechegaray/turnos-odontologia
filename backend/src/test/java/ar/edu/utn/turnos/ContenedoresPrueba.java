package ar.edu.utn.turnos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Contenedor de PostgreSQL real para las pruebas de integración.
 * Vive con el contexto de Spring (que se cachea entre clases), así que toda la suite
 * comparte un solo contenedor mientras nadie cambie la configuración del contexto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContenedoresPrueba {

    /** Misma imagen que {@code docker-compose.yml}: si una cambia, cambia la otra. */
    public static final String IMAGEN_POSTGRES = "postgres:16-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse(IMAGEN_POSTGRES));
    }
}
