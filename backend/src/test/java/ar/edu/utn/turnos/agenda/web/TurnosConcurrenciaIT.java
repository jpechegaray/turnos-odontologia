package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.jayway.jsonpath.JsonPath;
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
 * RN-15: dos recepciones crean a la vez un turno en el mismo hueco. Sin {@code @Transactional}: cada pedido
 * confirma su propia transacción, como en producción. Gane quien gane, el invariante es el mismo: un 201,
 * un 409 RN-01 y una sola fila. Se repite para que una carrera que pase "por suerte" no esconda una falla.
 */
@Import(RelojFijoConfig.class)
class TurnosConcurrenciaIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;
    private final List<Long> usuarios = new ArrayList<>();

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, TurnosApiIT.LUNES, "09:00", "13:00");
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
    @DisplayName("Dos creaciones simultáneas sobre el mismo hueco")
    void dosCreacionesSimultaneasSobreElMismoHueco() throws Exception {
        var cuerpo = "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\"}"
                .formatted(paciente, profesional, sillon, prestacion, TurnosApiIT.lunes("10:00"));
        // Sesión y CSRF se preparan antes de la largada, para que los dos pedidos salgan juntos.
        var recepcionA = recepcionista();
        var recepcionB = recepcionista();
        var csrfA = csrfValido();
        var csrfB = csrfValido();
        var largada = new CountDownLatch(1);

        var pedidoA = enHilo(largada, cuerpo, recepcionA, csrfA);
        var pedidoB = enHilo(largada, cuerpo, recepcionB, csrfB);
        largada.countDown();
        var respuestas = List.of(pedidoA.get(30, TimeUnit.SECONDS), pedidoB.get(30, TimeUnit.SECONDS));

        assertThat(respuestas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
        var rechazo = respuestas.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
        var json = rechazo.getContentAsString();
        assertThat((String) JsonPath.read(json, "$.codigo")).isEqualTo("RN-01");
        assertThat((String) JsonPath.read(json, "$.recurso")).isIn("PROFESIONAL", "SILLON");
        assertThat(jdbc.queryForObject("select count(*) from turno where profesional_id = ?", Integer.class, profesional))
                .isEqualTo(1);
    }

    private CompletableFuture<MockHttpServletResponse> enHilo(CountDownLatch largada, String cuerpo,
            RequestPostProcessor sesion, RequestPostProcessor csrf) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                largada.await(30, TimeUnit.SECONDS);
                return mockMvc.perform(post(TurnosApiIT.RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                        .with(sesion).with(csrf)).andReturn().getResponse();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
