package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.ReprogramarTurnoService;
import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoReprogramado;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /api/turnos/{id}/reprogramar} de punta a punta (US-011, Flujo 2, RN-18). El reloj fijo hace que
 * ahora sea el martes 2026-12-01 a las 09:00 en Buenos Aires; el profesional atiende todos los días de 08:00 a 20:00.
 */
@Transactional
@Import(RelojFijoConfig.class)
@RecordApplicationEvents
class TurnosReprogramacionApiIT extends PruebaIntegracionBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents eventos;

    @Autowired
    ReprogramarTurnoService servicio;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    RequestPostProcessor comoRecepcion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = profesionalQueAtiende();
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        comoRecepcion = sesionDe("RECEPCION");
    }

    Long profesionalQueAtiende() {
        var id = DatosAgenda.profesional(jdbc, true);
        for (int dia = 1; dia <= 7; dia++) {
            DatosAgenda.horario(jdbc, id, dia, "08:00", "20:00");
        }
        return id;
    }

    RequestPostProcessor sesionDe(String rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol);
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], Rol.valueOf(rol));
    }

    /** Fecha y hora de pared en Buenos Aires, como ISO con zona. */
    static String ba(String fechaHora) {
        return fechaHora + ":00-03:00";
    }

    static Intervalo intervalo(String desde, String hasta) {
        return new Intervalo(OffsetDateTime.parse(ba(desde)).toInstant(), OffsetDateTime.parse(ba(hasta)).toInstant());
    }

    Long turno(String desde, String hasta, String estado) {
        return turnoDe(profesional, sillon, desde, hasta, estado);
    }

    Long turnoDe(Long prof, Long sil, String desde, String hasta, String estado) {
        return DatosAgenda.turno(jdbc, paciente, prof, sil, prestacion, ba(desde), ba(hasta), estado);
    }

    /** El turno de referencia: jueves 3 de diciembre de 10:00 a 10:30, a 49 horas. */
    Long turnoDelJueves() {
        return turno("2026-12-03T10:00", "2026-12-03T10:30", "PROGRAMADO");
    }

    static MockHttpServletRequestBuilder reprogramacion(Object id) {
        return post("/api/turnos/{id}/reprogramar", id);
    }

    ResultActions reprogramar(Object id, String json, RequestPostProcessor quien) throws Exception {
        return mockMvc.perform(reprogramacion(id).with(quien).with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions reprogramar(Object id, String json) throws Exception {
        return reprogramar(id, json, comoRecepcion);
    }

    static String a(String fechaHora) {
        return "{\"inicio\":\"" + ba(fechaHora) + "\"}";
    }

    Map<String, Object> fila(Long id) {
        return jdbc.queryForMap("""
                select estado, profesional_id, sillon_id, duracion_min, reprogramado_tarde, version,
                       cast(inicio as text) as inicio
                from turno where id = ?
                """, id);
    }

    void sinEventos() {
        assertThat(eventos.stream(TurnoReprogramado.class)).isEmpty();
        assertThat(eventos.stream(HuecoLiberado.class)).isEmpty();
    }

    // --- Tarea 2.2: reprogramación ---

    @Test
    @DisplayName("Recepción mueve un turno a otro horario")
    void recepcionMueveUnTurno() throws Exception {
        var id = turnoDelJueves();

        reprogramar(id, a("2026-12-03T11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.inicio").value("2026-12-03T14:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-12-03T14:30:00Z"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.pacienteId").value(paciente))
                .andExpect(jsonPath("$.prestacionId").value(prestacion))
                .andExpect(jsonPath("$.reprogramadoTarde").value(false));

        assertThat(fila(id)).containsEntry("estado", "PROGRAMADO").containsEntry("reprogramado_tarde", false);
        assertThat(jdbc.queryForObject("select count(*) from turno where paciente_id = ?", Long.class, paciente))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Se puede cambiar de profesional, de sillón y de duración")
    void cambiaRecursosYDuracion() throws Exception {
        var id = turnoDelJueves();
        var otroProfesional = profesionalQueAtiende();
        DatosAgenda.asignarPrestacion(jdbc, otroProfesional, prestacion);
        var otroSillon = DatosAgenda.sillon(jdbc, true);

        reprogramar(id, """
                {"inicio":"%s","profesionalId":%d,"sillonId":%d,"duracionMin":45}
                """.formatted(ba("2026-12-03T15:00"), otroProfesional, otroSillon), sesionDe("ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(otroProfesional))
                .andExpect(jsonPath("$.sillonId").value(otroSillon))
                .andExpect(jsonPath("$.duracionMin").value(45))
                .andExpect(jsonPath("$.fin").value("2026-12-03T18:45:00Z"));
    }

    @Test
    @DisplayName("Un turno confirmado sigue confirmado")
    void confirmadoSigueConfirmado() throws Exception {
        var id = turno("2026-12-03T10:00", "2026-12-03T10:30", "CONFIRMADO");

        reprogramar(id, a("2026-12-04T10:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("Reprogramar al mismo horario no cambia nada")
    void mismoHorarioNoCambiaNada() throws Exception {
        var id = turnoDelJueves();
        var antes = fila(id);

        reprogramar(id, a("2026-12-03T10:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-12-03T13:00:00Z"));

        assertThat(fila(id)).isEqualTo(antes);
        sinEventos();
    }

    @Test
    @DisplayName("Un pedido sin inicio se rechaza")
    void pedidoSinInicio() throws Exception {
        var id = turnoDelJueves();
        var antes = fila(id);

        for (var cuerpo : List.of("{}", "{\"inicio\":\"jueves a las diez\"}", "{\"inicio\":")) {
            reprogramar(id, cuerpo).andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(fila(id)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Un turno inexistente no se puede reprogramar")
    void turnoInexistente() throws Exception {
        reprogramar(-1L, a("2026-12-03T11:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TURNO_INEXISTENTE"));
        sinEventos();
    }

    // --- Tarea 2.3: estado y validaciones del horario ---

    @ParameterizedTest
    @ValueSource(strings = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    @DisplayName("Un turno cancelado no se puede reprogramar")
    void estadoFinal(String estado) throws Exception {
        var id = turno("2026-12-03T10:00", "2026-12-03T10:30", estado);
        var antes = fila(id);

        reprogramar(id, a("2026-12-03T11:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));
        assertThat(fila(id)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Un turno que ya empezó no se puede reprogramar")
    void yaEmpezo() throws Exception {
        var id = turno("2026-12-01T09:00", "2026-12-01T09:30", "PROGRAMADO");
        var antes = fila(id);

        reprogramar(id, a("2026-12-03T11:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-05"));
        assertThat(fila(id)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Un turno se mueve quince minutos dentro de su propio intervalo")
    void quinceMinutosDentro() throws Exception {
        var id = turnoDelJueves();

        reprogramar(id, a("2026-12-03T10:15")).andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-12-03T13:15:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-12-03T13:45:00Z"));
    }

    @Test
    @DisplayName("Un horario ocupado por otro turno responde conflicto")
    void horarioOcupado() throws Exception {
        var id = turnoDelJueves();
        var otroSillon = DatosAgenda.sillon(jdbc, true);
        var otroProfesional = profesionalQueAtiende();
        // Ocupa al profesional en otro sillón y, aparte, al sillón con otro profesional.
        turnoDe(profesional, otroSillon, "2026-12-03T11:00", "2026-12-03T11:30", "PROGRAMADO");
        turnoDe(otroProfesional, sillon, "2026-12-03T13:00", "2026-12-03T13:30", "CONFIRMADO");
        var antes = fila(id);

        reprogramar(id, a("2026-12-03T11:15")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"));
        reprogramar(id, a("2026-12-03T13:15")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"));
        assertThat(fila(id)).isEqualTo(antes);
        sinEventos();
    }

    @Test
    @DisplayName("Un horario fuera de atención se rechaza")
    void fueraDeAtencion() throws Exception {
        var id = turnoDelJueves();
        DatosAgenda.bloqueo(jdbc, profesional, ba("2026-12-03T15:00"), ba("2026-12-03T16:00"));
        var antes = fila(id);

        reprogramar(id, a("2026-12-03T20:00")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-04"));
        reprogramar(id, a("2026-12-03T15:00")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-04"));
        assertThat(fila(id)).isEqualTo(antes);
    }

    @Test
    @DisplayName("El horario nuevo respeta el inicio futuro, la grilla y los recursos activos")
    void inicioGrillaYRecursos() throws Exception {
        var id = turnoDelJueves();
        var sillonInactivo = DatosAgenda.sillon(jdbc, false);
        var noRealiza = profesionalQueAtiende();
        var antes = fila(id);

        reprogramar(id, a("2026-12-01T08:30")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-13"));
        reprogramar(id, a("2026-12-03T11:03")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-09"));
        reprogramar(id, "{\"inicio\":\"%s\",\"sillonId\":%d}".formatted(ba("2026-12-03T11:00"), sillonInactivo))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("SILLON_INACTIVO"));
        reprogramar(id, "{\"inicio\":\"%s\",\"profesionalId\":%d}".formatted(ba("2026-12-03T11:00"), noRealiza))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RN-11"));
        assertThat(fila(id)).isEqualTo(antes);
        sinEventos();
    }

    // --- Tarea 2.4: tardía ---

    @Test
    @DisplayName("Reprogramar dentro del plazo queda marcado como tardío")
    void dentroDelPlazoEsTardio() throws Exception {
        var id = turno("2026-12-01T19:00", "2026-12-01T19:30", "PROGRAMADO");

        reprogramar(id, a("2026-12-04T10:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reprogramadoTarde").value(true));
        assertThat(fila(id)).containsEntry("reprogramado_tarde", true);
    }

    @Test
    @DisplayName("Reprogramar a 24 horas exactas no es tardío")
    void plazoExacto() throws Exception {
        var id = turno("2026-12-02T09:00", "2026-12-02T09:30", "PROGRAMADO");

        reprogramar(id, a("2026-12-04T10:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reprogramadoTarde").value(false));
    }

    @Test
    @DisplayName("La marca de tardía se conserva")
    void marcaSeConserva() throws Exception {
        var id = turno("2026-12-01T19:00", "2026-12-01T19:30", "PROGRAMADO");

        reprogramar(id, a("2026-12-04T10:00")).andExpect(jsonPath("$.reprogramadoTarde").value(true));
        reprogramar(id, a("2026-12-05T10:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.reprogramadoTarde").value(true));
    }

    // --- Tarea 2.5: liberación y eventos ---

    @Test
    @DisplayName("El horario anterior se puede volver a dar")
    void horarioAnteriorReutilizable() throws Exception {
        var id = turnoDelJueves();
        var nuevo = "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\"}"
                .formatted(DatosAgenda.paciente(jdbc, true), profesional, sillon, prestacion, ba("2026-12-03T10:00"));
        var alta = post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(nuevo);

        mockMvc.perform(alta.with(comoRecepcion).with(csrfValido())).andExpect(status().isConflict());
        reprogramar(id, a("2026-12-03T12:00")).andExpect(status().isOk());
        mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(nuevo)
                .with(comoRecepcion).with(csrfValido())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Reprogramar publica el turno reprogramado y el hueco anterior")
    void publicaEventos() throws Exception {
        var id = turnoDelJueves();

        reprogramar(id, a("2026-12-03T12:00")).andExpect(status().isOk());

        assertThat(eventos.stream(TurnoReprogramado.class)).singleElement().satisfies(e -> {
            assertThat(e.turnoId()).isEqualTo(id);
            assertThat(e.anterior()).isEqualTo(intervalo("2026-12-03T10:00", "2026-12-03T10:30"));
            assertThat(e.nuevo()).isEqualTo(intervalo("2026-12-03T12:00", "2026-12-03T12:30"));
            assertThat(e.profesionalAnteriorId()).isEqualTo(profesional);
            assertThat(e.reprogramadoTarde()).isFalse();
        });
        assertThat(eventos.stream(HuecoLiberado.class)).containsExactly(
                new HuecoLiberado(profesional, sillon, intervalo("2026-12-03T10:00", "2026-12-03T10:30"), id));
    }

    @Test
    @DisplayName("Moverse quince minutos publica solo el tramo que quedó libre")
    void publicaSoloElTramoLibre() throws Exception {
        var id = turnoDelJueves();

        reprogramar(id, a("2026-12-03T10:15")).andExpect(status().isOk());

        assertThat(eventos.stream(HuecoLiberado.class)).containsExactly(
                new HuecoLiberado(profesional, sillon, intervalo("2026-12-03T10:00", "2026-12-03T10:15"), id));
    }

    @Test
    @DisplayName("Una reprogramación rechazada no publica eventos")
    void rechazadaSinEventos() throws Exception {
        var cancelado = turno("2026-12-03T10:00", "2026-12-03T10:30", "CANCELADO");
        var empezado = turno("2026-12-01T09:00", "2026-12-01T09:30", "PROGRAMADO");
        var vigente = turno("2026-12-03T14:00", "2026-12-03T14:30", "PROGRAMADO");

        reprogramar(-1L, a("2026-12-03T11:00")).andExpect(status().isNotFound());
        reprogramar(cancelado, a("2026-12-03T11:00")).andExpect(status().isConflict());
        reprogramar(empezado, a("2026-12-03T11:00")).andExpect(status().isUnprocessableEntity());
        reprogramar(vigente, a("2026-12-03T20:00")).andExpect(status().isUnprocessableEntity());
        sinEventos();
    }

    // --- Tarea 3.2: acceso ---

    @Test
    @DisplayName("El odontólogo no puede reprogramar turnos")
    void odontologo() throws Exception {
        var id = turnoDelJueves();
        var antes = fila(id);
        var odontologo = sesionDe("ODONTOLOGO");

        reprogramar(id, a("2026-12-03T11:00"), odontologo).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        // 403 antes de validar el cuerpo.
        reprogramar(id, "{}", odontologo).andExpect(status().isForbidden());
        assertThat(fila(id)).isEqualTo(antes);
        sinEventos();
    }

    @Test
    @DisplayName("El servicio de reprogramación rechaza al odontólogo aunque se saltee el filtro HTTP")
    void servicioRechazaAlOdontologo() {
        var id = turnoDelJueves();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "odontologo-ficticio", null, List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO"))));
        try {
            assertThatThrownBy(() -> servicio.reprogramar(id, new ReprogramarTurnoService.Pedido(
                    OffsetDateTime.parse(ba("2026-12-03T11:00")).toInstant(), null, null, null)))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(fila(id)).containsEntry("estado", "PROGRAMADO");
    }

    @Test
    @DisplayName("Sin sesión no se puede reprogramar un turno")
    void sinSesion() throws Exception {
        var id = turnoDelJueves();
        var antes = fila(id);

        mockMvc.perform(reprogramacion(id).with(csrfValido()).contentType(MediaType.APPLICATION_JSON)
                .content(a("2026-12-03T11:00"))).andExpect(status().isUnauthorized());
        assertThat(fila(id)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Sin CSRF válido no se puede reprogramar un turno")
    void sinCsrf() throws Exception {
        var id = turnoDelJueves();
        var antes = fila(id);

        mockMvc.perform(reprogramacion(id).with(comoRecepcion).with(csrfConCabeceraIncorrecta())
                .contentType(MediaType.APPLICATION_JSON).content(a("2026-12-03T11:00")))
                .andExpect(status().isForbidden());
        assertThat(fila(id)).isEqualTo(antes);
    }
}
