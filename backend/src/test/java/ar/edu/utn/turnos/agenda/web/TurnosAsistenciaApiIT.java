package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.MarcarAsistenciaService;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /api/turnos/{id}/asistencia} de punta a punta con PostgreSQL real (C-29, RN-12, RN-20). El reloj
 * fijo hace que "ahora" sea el martes 1 de diciembre de 2026 a las 09:00 de Buenos Aires; los turnos se insertan
 * por SQL a una distancia conocida de ese instante.
 */
@Transactional
@Import(RelojFijoConfig.class)
class TurnosAsistenciaApiIT extends PruebaIntegracionBase {
    static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MarcarAsistenciaService marcarAsistencia;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    RequestPostProcessor comoRecepcion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        comoRecepcion = sesionDe("RECEPCION");
    }

    RequestPostProcessor sesionDe(String rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol);
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], Rol.valueOf(rol));
    }

    /** Turno de 30 minutos que empieza {@code desdeAhora} después del instante fijo (negativo: ya empezó). */
    Long turno(Duration desdeAhora, String estado) {
        var inicio = AHORA.plus(desdeAhora);
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), estado);
    }

    static MockHttpServletRequestBuilder asistencia(Long id) {
        return post("/api/turnos/{id}/asistencia", id);
    }

    ResultActions marcar(Long id, String json, RequestPostProcessor quien) throws Exception {
        return mockMvc.perform(asistencia(id).with(quien).with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions marcar(Long id, String json) throws Exception {
        return marcar(id, json, comoRecepcion);
    }

    static String estadoJson(String estado) {
        return "{\"estado\":\"" + estado + "\"}";
    }

    String estado(Long id) {
        return jdbc.queryForObject("select estado from turno where id = ?", String.class, id);
    }

    // --- Tarea 2.1: marca ---

    @Test
    @DisplayName("Recepción marca un turno como atendido")
    void recepcionMarcaUnTurnoComoAtendido() throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");

        marcar(id, estadoJson("ATENDIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ATENDIDO"));

        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    @Test
    @DisplayName("Recepción marca la ausencia de un turno confirmado")
    void recepcionMarcaLaAusenciaDeUnTurnoConfirmado() throws Exception {
        var id = turno(Duration.ofHours(-1), "CONFIRMADO");

        marcar(id, estadoJson("AUSENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AUSENTE"));

        assertThat(estado(id)).isEqualTo("AUSENTE");
    }

    @Test
    @DisplayName("A la hora exacta de inicio ya se puede marcar")
    void aLaHoraExactaDeInicioYaSePuedeMarcar() throws Exception {
        var id = turno(Duration.ZERO, "PROGRAMADO");

        marcar(id, estadoJson("ATENDIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDO"));

        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    @Test
    @DisplayName("Antes del inicio no se puede marcar")
    void antesDelInicioNoSePuedeMarcar() throws Exception {
        var id = turno(Duration.ofMinutes(1), "PROGRAMADO");

        marcar(id, estadoJson("AUSENTE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-20"));

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @ParameterizedTest(name = "cuerpo {0}")
    @ValueSource(strings = {"{\"estado\":\"CANCELADO\"}", "{\"estado\":\"CONFIRMADO\"}",
            "{\"estado\":\"PROGRAMADO\"}", "{}", "{\"estado\":null}"})
    @DisplayName("Un estado destino inválido se rechaza")
    void unEstadoDestinoInvalidoSeRechaza(String cuerpo) throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");

        marcar(id, cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("estado"));

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Un estado inexistente o un cuerpo ilegible da 400 y no 500")
    void unEstadoInexistenteOCuerpoIlegibleDa400() throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");

        marcar(id, estadoJson("FOO")).andExpect(status().isBadRequest());
        marcar(id, "{").andExpect(status().isBadRequest());
        mockMvc.perform(asistencia(id).with(comoRecepcion).with(csrfValido())).andExpect(status().isBadRequest());

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Un turno inexistente no admite asistencia")
    void unTurnoInexistenteNoAdmiteAsistencia() throws Exception {
        marcar(-1L, estadoJson("ATENDIDO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("TURNO_INEXISTENTE"));
    }

    // --- Tarea 2.2: corrección y cancelado ---

    @Test
    @DisplayName("Un turno cancelado no se marca")
    void unTurnoCanceladoNoSeMarca() throws Exception {
        var id = turno(Duration.ofHours(-1), "CANCELADO");

        marcar(id, estadoJson("ATENDIDO"), sesionDe("ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));
        // Para recepción también es 409 (no 403): cancelado no es una marca de asistencia.
        marcar(id, estadoJson("AUSENTE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));

        assertThat(estado(id)).isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("El administrador corrige una marca")
    void elAdministradorCorrigeUnaMarca() throws Exception {
        var id = turno(Duration.ofHours(-1), "AUSENTE");

        marcar(id, estadoJson("ATENDIDO"), sesionDe("ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDO"));

        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    @Test
    @DisplayName("Recepción no puede corregir una marca")
    void recepcionNoPuedeCorregirUnaMarca() throws Exception {
        var id = turno(Duration.ofHours(-1), "ATENDIDO");

        marcar(id, estadoJson("AUSENTE")).andExpect(status().isForbidden());
        // Aun pidiendo la misma marca, recepción recibe 403 sobre un estado final de asistencia.
        marcar(id, estadoJson("ATENDIDO")).andExpect(status().isForbidden());

        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    @Test
    @DisplayName("Repetir la misma marca se rechaza")
    void repetirLaMismaMarcaSeRechaza() throws Exception {
        var id = turno(Duration.ofHours(-1), "ATENDIDO");

        marcar(id, estadoJson("ATENDIDO"), sesionDe("ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));

        assertThat(estado(id)).isEqualTo("ATENDIDO");
    }

    // --- Tarea 3.1: acceso ---

    @Test
    @DisplayName("El odontólogo no puede marcar asistencia")
    void elOdontologoNoPuedeMarcarAsistencia() throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");
        var odontologo = sesionDe("ODONTOLOGO");

        marcar(id, estadoJson("ATENDIDO"), odontologo).andExpect(status().isForbidden());
        // 403 antes de validar el cuerpo: un estado inválido no cambia la respuesta.
        marcar(id, estadoJson("CANCELADO"), odontologo).andExpect(status().isForbidden());

        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("El servicio de asistencia rechaza al odontólogo aunque se saltee el filtro HTTP")
    void elServicioRechazaAlOdontologo() {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");
        var odontologo = new UsernamePasswordAuthenticationToken("odontologo-ficticio", null,
                List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO")));
        SecurityContextHolder.getContext().setAuthentication(odontologo);
        try {
            assertThatThrownBy(() -> marcarAsistencia.marcar(id, EstadoTurno.ATENDIDO, Rol.ODONTOLOGO))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Sin sesión no se puede marcar asistencia")
    void sinSesionNoSePuedeMarcarAsistencia() throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");

        mockMvc.perform(asistencia(id).with(csrfValido())
                        .contentType(MediaType.APPLICATION_JSON).content(estadoJson("ATENDIDO")))
                .andExpect(status().isUnauthorized());
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("Sin CSRF válido no se puede marcar asistencia")
    void sinCsrfValidoNoSePuedeMarcarAsistencia() throws Exception {
        var id = turno(Duration.ofHours(-1), "PROGRAMADO");

        mockMvc.perform(asistencia(id).with(comoRecepcion).with(csrfConCabeceraIncorrecta())
                        .contentType(MediaType.APPLICATION_JSON).content(estadoJson("ATENDIDO")))
                .andExpect(status().isForbidden());
        assertThat(estado(id)).isEqualTo("PROGRAMADO");
    }
}
