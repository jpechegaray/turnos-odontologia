package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /api/turnos/{id}/cancelar} de punta a punta con PostgreSQL real (US-012, Flujo 3). El reloj fijo
 * hace que "ahora" sea el martes 1 de diciembre de 2026 a las 09:00 de Buenos Aires; los turnos a cancelar se
 * insertan por SQL a una distancia conocida de ese instante. El plazo de cancelación es el de fábrica: 24 horas.
 */
@Transactional
@Import(RelojFijoConfig.class)
@RecordApplicationEvents
class TurnosCancelacionApiIT extends PruebaIntegracionBase {
    static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;
    static final int MARTES = 2;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents eventos;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long recepcionista;
    RequestPostProcessor comoRecepcion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, TurnosApiIT.LUNES, "09:00", "13:00");
        DatosAgenda.horario(jdbc, profesional, MARTES, "09:00", "13:00");
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        recepcionista = (Long) usuario[0];
        comoRecepcion = TurnosApiIT.sesion(recepcionista, (String) usuario[1], Rol.RECEPCION);
    }

    RequestPostProcessor sesionDe(String rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol);
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], Rol.valueOf(rol));
    }

    /** Turno de 30 minutos que empieza {@code desdeAhora} después del instante fijo. */
    Long turno(Duration desdeAhora, String estado) {
        var inicio = AHORA.plus(desdeAhora);
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), estado);
    }

    static MockHttpServletRequestBuilder cancelacion(Long id) {
        return post("/api/turnos/{id}/cancelar", id);
    }

    ResultActions cancelar(Long id, String json, RequestPostProcessor quien) throws Exception {
        var pedido = cancelacion(id).with(quien).with(csrfValido());
        if (json != null) {
            pedido = pedido.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(pedido);
    }

    ResultActions cancelar(Long id, String json) throws Exception {
        return cancelar(id, json, comoRecepcion);
    }

    Map<String, Object> fila(Long id) {
        return jdbc.queryForMap("""
                select estado, cancelacion_tardia, cancelado_en, cancelado_por, cancelado_por_usuario_id,
                       motivo_cancelacion
                from turno where id = ?
                """, id);
    }

    String estado(Long id) {
        return jdbc.queryForObject("select estado from turno where id = ?", String.class, id);
    }

    // --- Tarea 4.1: cancelación ---

    @Test
    @DisplayName("Recepción cancela un turno programado")
    void recepcionCancelaUnTurnoProgramado() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        cancelar(id, "{\"motivo\":\"El paciente avisó que no puede venir\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelacion").value("El paciente avisó que no puede venir"))
                .andExpect(jsonPath("$.cancelacionTardia").value(false))
                .andExpect(jsonPath("$.canceladoEn").value(AHORA.toString()))
                .andExpect(jsonPath("$.canceladoPor").value("USUARIO"));

        assertThat(fila(id)).containsEntry("estado", "CANCELADO")
                .containsEntry("motivo_cancelacion", "El paciente avisó que no puede venir")
                .containsEntry("cancelacion_tardia", false);
    }

    @Test
    @DisplayName("Se puede cancelar un turno confirmado")
    void sePuedeCancelarUnTurnoConfirmado() throws Exception {
        var id = turno(Duration.ofHours(72), "CONFIRMADO");

        cancelar(id, null, sesionDe("ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelacion").isEmpty());

        assertThat(fila(id)).containsEntry("estado", "CANCELADO").containsEntry("motivo_cancelacion", null);
    }

    @Test
    @DisplayName("Un motivo demasiado largo se rechaza")
    void unMotivoDemasiadoLargoSeRechaza() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        cancelar(id, "{\"motivo\":\"" + "m".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("motivo"));
        cancelar(id, "{").andExpect(status().isBadRequest());

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
        assertThat(eventos.stream(HuecoLiberado.class)).isEmpty();
    }

    @Test
    @DisplayName("Un turno inexistente no se puede cancelar")
    void unTurnoInexistenteNoSePuedeCancelar() throws Exception {
        cancelar(-1L, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TURNO_INEXISTENTE"));
    }

    @Test
    @DisplayName("La cancelación guarda quién la hizo y cuándo")
    void laCancelacionGuardaQuienLaHizoYCuando() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        cancelar(id, null).andExpect(status().isOk());

        var fila = fila(id);
        assertThat(fila).containsEntry("cancelado_por", "USUARIO")
                .containsEntry("cancelado_por_usuario_id", recepcionista);
        assertThat(((java.sql.Timestamp) fila.get("cancelado_en")).toInstant()).isEqualTo(AHORA);
    }

    // --- Tarea 4.2: estado y plazo ---

    @Test
    @DisplayName("Un turno ya cancelado no se vuelve a cancelar")
    void unTurnoYaCanceladoNoSeVuelveACancelar() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");
        cancelar(id, "{\"motivo\":\"Primera\"}").andExpect(status().isOk());
        var primera = fila(id);

        cancelar(id, "{\"motivo\":\"Segunda\"}", sesionDe("ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));

        assertThat(fila(id)).isEqualTo(primera);
    }

    @Test
    @DisplayName("Un turno atendido no se puede cancelar")
    void unTurnoAtendidoNoSePuedeCancelar() throws Exception {
        var id = turno(Duration.ofHours(-2), "ATENDIDO");

        cancelar(id, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));
        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    @Test
    @DisplayName("Un turno que ya empezó no se puede cancelar")
    void unTurnoQueYaEmpezoNoSePuedeCancelar() throws Exception {
        var id = turno(Duration.ZERO, "PROGRAMADO");

        cancelar(id, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-05"));
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Por la API, cancelar a 24 horas exactas no es tardío y un minuto después sí")
    void bordesDelPlazoPorLaApi() throws Exception {
        var aPlazo = turno(Duration.ofHours(24), "PROGRAMADO");
        // Otro profesional y otro sillón: los dos turnos se superponen y no deben chocar entre sí.
        var dentro = DatosAgenda.turno(jdbc, paciente, DatosAgenda.profesional(jdbc, true), DatosAgenda.sillon(jdbc, true),
                prestacion,
                AHORA.plus(Duration.ofHours(24)).minus(Duration.ofMinutes(1)).toString(),
                AHORA.plus(Duration.ofHours(24)).plus(Duration.ofMinutes(29)).toString(), "PROGRAMADO");

        cancelar(aPlazo, null).andExpect(jsonPath("$.cancelacionTardia").value(false));
        cancelar(dentro, null).andExpect(jsonPath("$.cancelacionTardia").value(true));
    }

    @Test
    @DisplayName("Un cambio del plazo no altera cancelaciones anteriores")
    void unCambioDelPlazoNoAlteraCancelacionesAnteriores() throws Exception {
        var anterior = turno(Duration.ofHours(48), "PROGRAMADO");
        cancelar(anterior, null).andExpect(jsonPath("$.cancelacionTardia").value(false));

        jdbc.update("""
                insert into configuracion (clave, valor, descripcion, actualizado_en)
                values ('cancelacion.plazo_horas', '72', 'Plazo ficticio', now())
                on conflict (clave) do update set valor = excluded.valor
                """);
        var posterior = turno(Duration.ofHours(49), "PROGRAMADO");
        cancelar(posterior, null).andExpect(jsonPath("$.cancelacionTardia").value(true));

        assertThat(fila(anterior)).containsEntry("cancelacion_tardia", false);
    }

    // --- Tarea 4.3: el intervalo queda libre ---

    String nuevoTurno(String inicioIso) {
        return "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\"}"
                .formatted(paciente, profesional, sillon, prestacion, inicioIso);
    }

    ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json)
                .with(comoRecepcion).with(csrfValido()));
    }

    @Test
    @DisplayName("Tras cancelar se puede dar otro turno en el mismo horario")
    void trasCancelarSePuedeDarOtroTurnoEnElMismoHorario() throws Exception {
        var id = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, TurnosApiIT.lunes("10:00"),
                TurnosApiIT.lunes("10:30"), "PROGRAMADO");
        crear(nuevoTurno(TurnosApiIT.lunes("10:00"))).andExpect(status().isConflict());

        cancelar(id, null).andExpect(status().isOk());

        crear(nuevoTurno(TurnosApiIT.lunes("10:00"))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Una cancelación tardía también libera el horario")
    void unaCancelacionTardiaTambienLiberaElHorario() throws Exception {
        // Martes 1 de diciembre de 2026, 10:00 en Buenos Aires: una hora después del instante fijo.
        var inicio = "2026-12-01T10:00:00-03:00";
        var id = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio,
                "2026-12-01T10:30:00-03:00", "PROGRAMADO");

        cancelar(id, null).andExpect(status().isOk()).andExpect(jsonPath("$.cancelacionTardia").value(true));

        crear(nuevoTurno(inicio)).andExpect(status().isCreated());
    }

    // --- Tarea 4.4: avisos ---

    @Test
    @DisplayName("Una cancelación publica un único hueco liberado")
    void unaCancelacionPublicaUnUnicoHuecoLiberado() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        cancelar(id, null).andExpect(status().isOk());

        var intervalo = Intervalo.desde(AHORA.plus(Duration.ofHours(72)), 30);
        assertThat(eventos.stream(TurnoCancelado.class)).singleElement().satisfies(e -> {
            assertThat(e.turnoId()).isEqualTo(id);
            assertThat(e.intervalo()).isEqualTo(intervalo);
            assertThat(e.cancelacionTardia()).isFalse();
        });
        assertThat(eventos.stream(HuecoLiberado.class)).singleElement().satisfies(e -> {
            assertThat(e.turnoOrigenId()).isEqualTo(id);
            assertThat(e.profesionalId()).isEqualTo(profesional);
            assertThat(e.sillonId()).isEqualTo(sillon);
            assertThat(e.hueco()).isEqualTo(intervalo);
        });
    }

    @Test
    @DisplayName("Una cancelación rechazada no publica avisos")
    void unaCancelacionRechazadaNoPublicaAvisos() throws Exception {
        var cancelado = turno(Duration.ofHours(72), "CANCELADO");
        var iniciado = turno(Duration.ZERO, "PROGRAMADO");

        cancelar(cancelado, null).andExpect(status().isConflict());
        cancelar(iniciado, null).andExpect(status().isUnprocessableEntity());

        assertThat(eventos.stream(TurnoCancelado.class)).isEmpty();
        assertThat(eventos.stream(HuecoLiberado.class)).isEmpty();
    }

    // --- Tarea 6.1: acceso ---

    @Autowired
    ar.edu.utn.turnos.agenda.application.CancelarTurnoService cancelarTurno;

    @Test
    @DisplayName("El odontólogo no puede cancelar turnos")
    void elOdontologoNoPuedeCancelarTurnos() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");
        var odontologo = sesionDe("ODONTOLOGO");

        cancelar(id, null, odontologo).andExpect(status().isForbidden());
        // 403 antes de validar el cuerpo: un motivo inválido no cambia la respuesta.
        cancelar(id, "{\"motivo\":\"" + "m".repeat(256) + "\"}", odontologo).andExpect(status().isForbidden());

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
        assertThat(eventos.stream(HuecoLiberado.class)).isEmpty();
    }

    @Test
    @DisplayName("El servicio de cancelación rechaza al odontólogo aunque se saltee el filtro HTTP")
    void elServicioRechazaAlOdontologo() {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");
        var odontologo = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "odontologo-ficticio", null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ODONTOLOGO")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(odontologo);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> cancelarTurno.cancelarPorPersonal(id, null, null))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Sin sesión no se puede cancelar un turno")
    void sinSesionNoSePuedeCancelarUnTurno() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        mockMvc.perform(cancelacion(id).with(csrfValido())).andExpect(status().isUnauthorized());
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Sin CSRF válido no se puede cancelar un turno")
    void sinCsrfValidoNoSePuedeCancelarUnTurno() throws Exception {
        var id = turno(Duration.ofHours(72), "PROGRAMADO");

        mockMvc.perform(cancelacion(id).with(comoRecepcion).with(csrfConCabeceraIncorrecta()))
                .andExpect(status().isForbidden());
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }
}
