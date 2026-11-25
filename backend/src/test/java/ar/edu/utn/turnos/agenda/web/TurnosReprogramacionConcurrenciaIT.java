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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Una reprogramación y un alta apuntan a la vez al mismo horario libre (C-35, RN-15). Sin {@code @Transactional}:
 * cada pedido confirma su propia transacción. Los advisory locks ordenan los dos pedidos y, si no, el
 * {@code EXCLUDE} de la base: gana uno y el otro recibe {@code 409 RN-01}.
 */
@Import(RelojFijoConfig.class)
class TurnosReprogramacionConcurrenciaIT extends PruebaIntegracionBase {
    /** Jueves 3 de diciembre de 2026 a las 12:00 en Buenos Aires. */
    private static final String MEDIODIA = "2026-12-03T12:00:00-03:00";

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long otroPaciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;
    private Long turno;
    private final List<Long> usuarios = new ArrayList<>();

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        otroPaciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        for (int dia = 1; dia <= 7; dia++) {
            DatosAgenda.horario(jdbc, profesional, dia, "08:00", "20:00");
        }
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, "2026-12-03T10:00:00-03:00",
                "2026-12-03T10:30:00-03:00", "PROGRAMADO");
    }

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        jdbc.update("delete from pacientes where id = ?", otroPaciente);
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
    @DisplayName("Una reprogramación y un alta compiten por el mismo horario")
    void reprogramacionYAltaCompiten() throws Exception {
        var reprogramacion = post("/api/turnos/{id}/reprogramar", turno).contentType(MediaType.APPLICATION_JSON)
                .content("{\"inicio\":\"" + MEDIODIA + "\"}").with(recepcionista()).with(csrfValido());
        var alta = post("/api/turnos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\"}"
                        .formatted(otroPaciente, profesional, sillon, prestacion, MEDIODIA))
                .with(recepcionista()).with(csrfValido());
        var largada = new CountDownLatch(1);

        var pedidoA = enHilo(largada, reprogramacion);
        var pedidoB = enHilo(largada, alta);
        largada.countDown();
        var respuestas = List.of(pedidoA.get(30, TimeUnit.SECONDS), pedidoB.get(30, TimeUnit.SECONDS));

        var estados = respuestas.stream().map(MockHttpServletResponse::getStatus).toList();
        assertThat(estados).containsAnyOf(200, 201).contains(409);
        var rechazo = respuestas.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
        assertThat((String) JsonPath.read(rechazo.getContentAsString(), "$.codigo")).isEqualTo("RN-01");
        assertThat(jdbc.queryForObject("""
                select count(*) from turno
                where profesional_id = ? and estado <> 'CANCELADO' and inicio = cast(? as timestamptz)
                """, Long.class, profesional, MEDIODIA)).isEqualTo(1L);
    }

    private CompletableFuture<MockHttpServletResponse> enHilo(CountDownLatch largada,
            MockHttpServletRequestBuilder pedido) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                largada.await(30, TimeUnit.SECONDS);
                return mockMvc.perform(pedido).andReturn().getResponse();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
