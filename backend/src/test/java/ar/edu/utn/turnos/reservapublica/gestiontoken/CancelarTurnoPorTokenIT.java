package ar.edu.utn.turnos.reservapublica.gestiontoken;

import static ar.edu.utn.turnos.reservapublica.gestiontoken.GestionTurnoApiIT.AHORA;
import static ar.edu.utn.turnos.reservapublica.gestiontoken.GestionTurnoApiIT.LIMITE;
import static ar.edu.utn.turnos.reservapublica.gestiontoken.GestionTurnoApiIT.RUTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

@Import({RelojFijoConfig.class, CancelarTurnoPorTokenIT.Avisos.class})
class CancelarTurnoPorTokenIT extends PruebaIntegracionBase {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ServicioTokenTurno tokens;
    @Autowired
    Avisos avisos;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long turno;

    @AfterEach
    void limpiar() {
        jdbc.update("delete from token_turno where turno_id = ?", turno);
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        avisos.cancelados.clear();
        avisos.huecos.clear();
    }

    @Test
    @DisplayName("El paciente cancela en plazo y el intervalo se puede volver a reservar")
    void enPlazo() throws Exception {
        preparar(LIMITE, "PROGRAMADO");
        String token = tokens.emitir(turno).claro();
        verificarCruceInicio(token);
        moverInicio(LIMITE);
        mockMvc.perform(post(RUTA + "/cancelar", token)).andExpect(status().isNoContent());
        verificarAuditoria(false);
        assertThat(insertar(LIMITE, "PROGRAMADO")).isNotEqualTo(turno);
    }

    @Test
    @DisplayName("Cancelar tarde un turno confirmado libera el intervalo y revoca sus enlaces")
    void tarde() throws Exception {
        Instant inicio = LIMITE.minusSeconds(60);
        preparar(inicio, "CONFIRMADO");
        String primero = tokens.emitir(turno).claro();
        String segundo = tokens.emitir(turno).claro();
        mockMvc.perform(post(RUTA + "/cancelar", primero)).andExpect(status().isNoContent());
        verificarAuditoria(true);
        assertThat(insertar(inicio, "PROGRAMADO")).isNotEqualTo(turno);
        assertThat(jdbc.queryForList("select revocado_en from token_turno where turno_id = ?",
                Timestamp.class, turno)).hasSize(2).allSatisfy(marca -> assertThat(marca.toInstant())
                        .isEqualTo(AHORA));
        for (String token : List.of(primero, segundo)) {
            mockMvc.perform(get(RUTA, token)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("TOKEN_TURNO_NO_ENCONTRADO"));
        }
    }

    private void verificarCruceInicio(String token) throws Exception {
        // Simula el borde: C-32 ya autorizó, pero agenda ve que el turno empezó.
        moverInicio(AHORA);
        mockMvc.perform(post(RUTA + "/cancelar", token)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TOKEN_TURNO_NO_ENCONTRADO"));
        assertThat(jdbc.queryForObject("select estado from turno where id = ?", String.class, turno))
                .isEqualTo("PROGRAMADO");
        assertThat(jdbc.queryForObject("select ultimo_uso_en from token_turno where token_hash = ?",
                Timestamp.class, new ServicioToken().hashear(token))).isNull();
        assertThat(avisos.cancelados).isEmpty();
        assertThat(avisos.huecos).isEmpty();
    }

    private void moverInicio(Instant inicio) {
        jdbc.update("update turno set inicio = ?, fin = ? where id = ?", Timestamp.from(inicio),
                Timestamp.from(inicio.plusSeconds(1800)), turno);
    }

    @Test
    @DisplayName("Dos cancelaciones públicas simultáneas liberan un solo hueco")
    void simultaneas() throws Exception {
        preparar(LIMITE, "PROGRAMADO");
        String primero = tokens.emitir(turno).claro();
        String segundo = tokens.emitir(turno).claro();
        var preparados = new CountDownLatch(2);
        try (var solicitudes = Executors.newFixedThreadPool(2)) {
            var a = solicitudes.submit(() -> cancelarJuntoConOtro(primero, preparados));
            var b = solicitudes.submit(() -> cancelarJuntoConOtro(segundo, preparados));
            var respuestas = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertThat(respuestas).extracting(MockHttpServletResponse::getStatus)
                    .containsExactlyInAnyOrder(204, 404);
            var rechazo = respuestas.stream().filter(r -> r.getStatus() == 404).findFirst().orElseThrow();
            assertThat(rechazo.getContentAsString()).contains("TOKEN_TURNO_NO_ENCONTRADO")
                    .doesNotContain(primero, segundo);
        }
        assertThat(avisos.cancelados).singleElement().satisfies(evento -> assertThat(evento.turnoId()).isEqualTo(turno));
        assertThat(avisos.huecos).singleElement().satisfies(evento -> assertThat(evento.turnoOrigenId()).isEqualTo(turno));
    }

    private MockHttpServletResponse cancelarJuntoConOtro(String token, CountDownLatch preparados) throws Exception {
        preparados.countDown();
        assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post(RUTA + "/cancelar", token)).andReturn().getResponse();
    }

    private void preparar(Instant inicio, String estado) {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        turno = insertar(inicio, estado);
    }

    private Long insertar(Instant inicio, String estado) {
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plusSeconds(1800).toString(), estado);
    }

    private void verificarAuditoria(boolean tardia) {
        var fila = jdbc.queryForMap("""
                select estado, cancelado_por, cancelado_en, cancelado_por_usuario_id, motivo_cancelacion,
                       cancelacion_tardia from turno where id = ?
                """, turno);
        assertThat(fila).containsEntry("estado", "CANCELADO").containsEntry("cancelado_por", "PACIENTE_TOKEN")
                .containsEntry("cancelado_en", Timestamp.from(AHORA))
                .containsEntry("cancelado_por_usuario_id", null).containsEntry("motivo_cancelacion", null)
                .containsEntry("cancelacion_tardia", tardia);
    }

    @TestConfiguration
    static class Avisos {
        final List<TurnoCancelado> cancelados = new CopyOnWriteArrayList<>();
        final List<HuecoLiberado> huecos = new CopyOnWriteArrayList<>();

        @EventListener
        void cancelado(TurnoCancelado evento) {
            cancelados.add(evento);
        }

        @EventListener
        void hueco(HuecoLiberado evento) {
            huecos.add(evento);
        }
    }
}
