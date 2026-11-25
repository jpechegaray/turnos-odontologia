package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Dos recepciones cancelan a la vez el mismo turno (C-27, design D5). Sin {@code @Transactional}: cada pedido
 * confirma su propia transacción. El lock de fila hace que una gane (200) y la otra vea el turno ya cancelado
 * (409), con un solo {@link HuecoLiberado}. {@code ApplicationEvents} solo ve el hilo del test, por eso los
 * avisos se cuentan con un oyente propio.
 */
@Import({RelojFijoConfig.class, TurnosCancelacionConcurrenciaIT.Avisos.class})
class TurnosCancelacionConcurrenciaIT extends PruebaIntegracionBase {

    @TestConfiguration
    static class Avisos {
        final List<HuecoLiberado> huecos = new CopyOnWriteArrayList<>();

        @EventListener
        void alLiberar(HuecoLiberado hueco) {
            huecos.add(hueco);
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Avisos avisos;

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
        var inicio = RelojFijoConfig.INSTANTE_FIJO.plus(Duration.ofHours(72));
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), "PROGRAMADO");
        avisos.huecos.clear();
    }

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        for (Long usuario : usuarios) {
            jdbc.update("delete from usuario where id = ?", usuario);
        }
        usuarios.clear();
    }

    private RequestPostProcessor recepcionista() {
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        usuarios.add((Long) usuario[0]);
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], Rol.RECEPCION);
    }

    @RepeatedTest(5)
    @DisplayName("Dos cancelaciones simultáneas del mismo turno")
    void dosCancelacionesSimultaneasDelMismoTurno() throws Exception {
        var recepcionA = recepcionista();
        var recepcionB = recepcionista();
        var csrfA = csrfValido();
        var csrfB = csrfValido();
        var largada = new CountDownLatch(1);

        var pedidoA = enHilo(largada, recepcionA, csrfA);
        var pedidoB = enHilo(largada, recepcionB, csrfB);
        largada.countDown();
        var respuestas = List.of(pedidoA.get(30, TimeUnit.SECONDS), pedidoB.get(30, TimeUnit.SECONDS));

        assertThat(respuestas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(200, 409);
        var rechazo = respuestas.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
        assertThat((String) JsonPath.read(rechazo.getContentAsString(), "$.codigo"))
                .isIn("TURNO_TRANSICION_INVALIDA", "TURNO_MODIFICADO");
        assertThat(jdbc.queryForObject("select estado from turno where id = ?", String.class, turno))
                .isEqualTo("CANCELADO");
        assertThat(avisos.huecos).singleElement().satisfies(h -> assertThat(h.turnoOrigenId()).isEqualTo(turno));
    }

    private CompletableFuture<MockHttpServletResponse> enHilo(CountDownLatch largada, RequestPostProcessor sesion,
            RequestPostProcessor csrf) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                largada.await(30, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/turnos/{id}/cancelar", turno).with(sesion).with(csrf))
                        .andReturn().getResponse();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
