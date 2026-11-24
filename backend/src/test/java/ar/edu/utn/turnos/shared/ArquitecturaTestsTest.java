package ar.edu.utn.turnos.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reglas de arquitectura de los tests. Se ejecuta en surefire, sin contexto de Spring ni contenedores.
 */
class ArquitecturaTestsTest {

    private static final Path RAIZ_TESTS = Path.of("src", "test", "java");

    @Test
    @DisplayName("Ningún test de integración usa csrf() de spring-security-test")
    void ningunTestDeIntegracionUsaCsrfDeSpringSecurityTest() throws IOException {
        // csrf() reemplaza para siempre el CsrfTokenRepository del contexto compartido y rompe
        // GET /api/auth/csrf (cookie XSRF-TOKEN) en las clases siguientes. Se usa PruebaIntegracionBase.csrfValido().
        List<Path> infractores;
        try (Stream<Path> archivos = Files.walk(RAIZ_TESTS)) {
            infractores = archivos
                    .filter(ruta -> ruta.getFileName().toString().endsWith("IT.java"))
                    .filter(ArquitecturaTestsTest::usaCsrfDeSpringSecurityTest)
                    .toList();
        }

        assertThat(infractores)
                .as("Tests de integración que usan SecurityMockMvcRequestPostProcessors.csrf()")
                .isEmpty();
    }

    private static boolean usaCsrfDeSpringSecurityTest(Path ruta) {
        try {
            return Files.readString(ruta).contains("SecurityMockMvcRequestPostProcessors.csrf");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + ruta, e);
        }
    }
}
