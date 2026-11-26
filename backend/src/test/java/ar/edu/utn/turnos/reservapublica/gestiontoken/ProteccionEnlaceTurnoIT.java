package ar.edu.utn.turnos.reservapublica.gestiontoken;

import static ar.edu.utn.turnos.reservapublica.gestiontoken.GestionTurnoApiIT.LIMITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.TurnosParaGestionPublica;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.buffered=false",
    "app.ratelimit.max-solicitudes-por-ip=9"
})
@Import(RelojFijoConfig.class)
@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
class ProteccionEnlaceTurnoIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/public/turnos/";

    @TempDir
    static Path registros;

    @DynamicPropertySource
    static void directorio(DynamicPropertyRegistry props) {
        props.add("server.tomcat.accesslog.directory", () -> registros.toString());
    }

    @LocalServerPort
    int puerto;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ServicioTokenTurno tokens;
    @MockitoSpyBean
    TurnosParaGestionPublica agenda;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long turno;
    String token;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, LIMITE.toString(),
                LIMITE.plusSeconds(1800).toString(), "PROGRAMADO");
        token = tokens.emitir(turno).claro();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from token_turno where turno_id = ?", turno);
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
    }

    @Test
    @DisplayName("Ninguna respuesta ni registro expone el token del enlace")
    void protegeRespuestaYRegistros(CapturedOutput salida) throws Exception {
        String ajeno = new ServicioToken().generar().claro();
        try (var cliente = HttpClient.newHttpClient()) {
            verificar(get(cliente, token), 200, token);
            verificar(post(cliente, token + "/confirmar"), 200, token);
            verificar(post(cliente, token + "/confirmar"), 409, token);
            verificar(get(cliente, ajeno), 404, ajeno);
            verificar(get(cliente, token + "/no-existe"), 404, token);
            verificar(get(cliente, token + "/confirmar"), 405, token);
            verificar(get(cliente, token + ";invalido"), 400, token);
            doThrow(new IllegalStateException("Fallo ficticio con URI " + RUTA + token)).when(agenda).consultar(turno);
            verificar(get(cliente, token), 500, token);
            verificar(post(cliente, token + "/cancelar"), 204, token);
            verificar(get(cliente, token), 429, token);
        }
        assertThat(salida.getAll()).contains("Error inesperado").doesNotContain(token, ajeno);
        try (var archivos = Files.list(registros)) {
            var logs = archivos.filter(Files::isRegularFile).toList();
            assertThat(logs).isNotEmpty();
            for (var log : logs) {
                assertThat(Files.readString(log)).contains(" 200 ", " 404 ", " 429 ", " 500 ")
                        .doesNotContain(token, ajeno);
            }
        }
    }

    private HttpResponse<String> get(HttpClient cliente, String ruta) throws Exception {
        return enviar(cliente, ruta, "GET");
    }

    private HttpResponse<String> post(HttpClient cliente, String ruta) throws Exception {
        return enviar(cliente, ruta, "POST");
    }

    private HttpResponse<String> enviar(HttpClient cliente, String ruta, String metodo) throws Exception {
        var uri = URI.create("http://localhost:" + puerto + RUTA + ruta);
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
