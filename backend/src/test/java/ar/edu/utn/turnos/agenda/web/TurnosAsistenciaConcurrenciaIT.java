package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Recepción marca ATENDIDO y el administrador marca AUSENTE a la vez sobre el mismo turno ya iniciado (C-29,
 * design D2). Sin {@code @Transactional}: cada pedido confirma su propia transacción. El lock de fila serializa
 * las dos marcas: si gana recepción, el administrador corrige (200/200); si gana el administrador, recepción ve
 * una marca final y recibe 403. Un conflicto de versión daría 409. Nunca un 500.
 */
@Import(RelojFijoConfig.class)
class TurnosAsistenciaConcurrenciaIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;
    private Long turno;
    private final List<Long> usuarios = new ArrayList<>();

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        var inicio = RelojFijoConfig.INSTANTE_FIJO.minus(Duration.ofHours(1));
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), "PROGRAMADO");
    }

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        for (Long usuario : usuarios) {
            jdbc.update("delete from usuario where id = ?", usuario);
        }
        usuarios.clear();
    }

    private RequestPostProcessor sesionDe(Rol rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol.name());
        usuarios.add((Long) usuario[0]);
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], rol);
    }

    @RepeatedTest(5)
    @DisplayName("Dos marcas de asistencia simultáneas del mismo turno se serializan")
    void dosMarcasSimultaneasDelMismoTurnoSeSerializan() throws Exception {
        var recepcion = sesionDe(Rol.RECEPCION);
        var admin = sesionDe(Rol.ADMIN);
        var csrfRecepcion = csrfValido();
        var csrfAdmin = csrfValido();
        var largada = new CountDownLatch(1);

        var pedidoRecepcion = enHilo(largada, recepcion, csrfRecepcion, "ATENDIDO");
        var pedidoAdmin = enHilo(largada, admin, csrfAdmin, "AUSENTE");
        largada.countDown();
        var deRecepcion = pedidoRecepcion.get(30, TimeUnit.SECONDS);
        var deAdmin = pedidoAdmin.get(30, TimeUnit.SECONDS);

        var codigos = List.of(deRecepcion.getStatus(), deAdmin.getStatus());
        assertThat(codigos).doesNotContain(500);
        // Orden: [recepción, administrador].
        assertThat(codigos).as("combinación de respuestas").isIn(
                List.of(200, 200), List.of(403, 200), List.of(200, 409), List.of(409, 200));
        for (var respuesta : List.of(deRecepcion, deAdmin)) {
            if (respuesta.getStatus() == 409) {
                assertThat((String) JsonPath.read(respuesta.getContentAsString(), "$.codigo"))
                        .isIn("TURNO_TRANSICION_INVALIDA", "TURNO_MODIFICADO");
            }
        }
        var estadoFinal = jdbc.queryForObject("select estado from turno where id = ?", String.class, turno);
        assertThat(estadoFinal).isIn("ATENDIDO", "AUSENTE");
        if (codigos.equals(List.of(200, 200))) {
            // Recepción marcó primero y el administrador corrigió después.
            assertThat(estadoFinal).isEqualTo("AUSENTE");
        }
    }

    private CompletableFuture<MockHttpServletResponse> enHilo(CountDownLatch largada, RequestPostProcessor sesion,
            RequestPostProcessor csrf, String estado) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                largada.await(30, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/turnos/{id}/asistencia", turno).with(sesion).with(csrf)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"))
                        .andReturn().getResponse();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
