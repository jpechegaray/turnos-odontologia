package ar.edu.utn.turnos.listaespera.oferta.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Protección del token de la oferta en la ruta (C-42, spec toma-oferta-hueco; RN-25, RN-38): servidor real, con el
 * registro de accesos de Tomcat encendido, para revisar respuestas, logs de la aplicación y logs de acceso.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.buffered=false"
})
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class ProteccionEnlaceOfertaIT extends EscenarioTomaOfertaIT {
    private static final String BASE = "/api/public/ofertas/";
    private static final Pattern TOKEN_RESPUESTA = Pattern.compile("\"token\":\"([A-Za-z0-9_-]{43})\"");

    @TempDir
    static Path registros;

    @DynamicPropertySource
    static void directorio(DynamicPropertyRegistry props) {
        props.add("server.tomcat.accesslog.directory", () -> registros.toString());
    }

    @LocalServerPort
    int puerto;

    @Test
    @DisplayName("Las respuestas no se guardan en caché")
    void sinCache() throws Exception {
        var token = ofertaDisponible();
        var ajeno = tokens.generar().claro();
        try (var cliente = HttpClient.newHttpClient()) {
            verificar(enviar(cliente, token, "GET"), 200, token);
            verificar(enviar(cliente, token + "/tomar", "POST"), 201, token);
            var noEncontrada = enviar(cliente, ajeno, "GET");
            verificar(noEncontrada, 404, ajeno);
            assertThat(noEncontrada.body()).contains("\"instance\":\"/api/public/ofertas\"");
            verificar(enviar(cliente, ajeno + "/tomar", "POST"), 404, ajeno);
            verificar(enviar(cliente, token + "/no-existe", "GET"), 404, token);
        }
    }

    @Test
    @DisplayName("Los tokens no aparecen en los logs")
    void sinTokensEnLogs(CapturedOutput salida) throws Exception {
        var token = ofertaDisponible();
        String tokenTurno;
        try (var cliente = HttpClient.newHttpClient()) {
            verificar(enviar(cliente, token, "GET"), 200, token);
            var tomada = enviar(cliente, token + "/tomar", "POST");
            verificar(tomada, 201, token);
            var m = TOKEN_RESPUESTA.matcher(tomada.body());
            assertThat(m.find()).isTrue();
            tokenTurno = m.group(1);
        }

        assertThat(salida.getAll()).contains("tomada").doesNotContain(token, tokenTurno);
        try (var archivos = Files.list(registros)) {
            var logs = archivos.filter(Files::isRegularFile).toList();
            assertThat(logs).isNotEmpty();
            for (var log : logs) {
                assertThat(Files.readString(log)).doesNotContain(token, tokenTurno);
            }
        }
    }

    private HttpResponse<String> enviar(HttpClient cliente, String ruta, String metodo) throws Exception {
        var uri = URI.create("http://localhost:" + puerto + BASE + ruta);
        return cliente.send(HttpRequest.newBuilder(uri).method(metodo, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void verificar(HttpResponse<String> respuesta, int estado, String secreto) {
        assertThat(respuesta.statusCode()).isEqualTo(estado);
        assertThat(respuesta.headers().allValues("Cache-Control")).contains("no-store");
        assertThat(respuesta.body()).doesNotContain(secreto);
        assertThat(respuesta.headers().toString()).doesNotContain(secreto);
    }
}
