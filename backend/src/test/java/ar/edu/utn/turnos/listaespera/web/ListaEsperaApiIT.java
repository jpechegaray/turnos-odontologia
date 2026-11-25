package ar.edu.utn.turnos.listaespera.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** El reloj fijo de {@link RelojFijoConfig} hace que hoy sea 2026-12-01 en Buenos Aires. */
@Transactional
@Import(RelojFijoConfig.class)
class ListaEsperaApiIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/lista-espera";
    private static final String DETALLE = RUTA + "/{id}";
    private static final String PERSONAL = "personal-ficticio";

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long prestacion;
    private Long profesional;

    @BeforeEach
    void preparar() {
        paciente = DatosFicticios.paciente(jdbc, "lista@correo.test");
        prestacion = DatosFicticios.prestacion(jdbc, "Limpieza lista");
        profesional = DatosFicticios.profesional(jdbc, "MP-LISTA", true);
    }

    // --- Alta ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Recepción anota a un paciente con todos los datos")
    void altaCompleta(String rol) throws Exception {
        var cuerpo = json(Map.of("pacienteId", paciente, "prestacionId", prestacion, "profesionalId", profesional,
                "fechaDesde", "2026-12-05", "fechaHasta", "2026-12-20", "franjaDesde", "09:00", "franjaHasta", "12:00"));
        mockMvc.perform(alta(cuerpo).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pacienteId").value(paciente))
                .andExpect(jsonPath("$.prestacionId").value(prestacion))
                .andExpect(jsonPath("$.profesionalId").value(profesional))
                .andExpect(jsonPath("$.fechaDesde").value("2026-12-05"))
                .andExpect(jsonPath("$.fechaHasta").value("2026-12-20"))
                .andExpect(jsonPath("$.franjaDesde").value("09:00"))
                .andExpect(jsonPath("$.franjaHasta").value("12:00"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.turnoAsignadoId").value((Object) null))
                .andExpect(jsonPath("$.creadoEn").value("2026-12-01T12:00:00Z"));
        assertThat(jdbc.queryForObject("select franja_desde::text from lista_espera where paciente_id = ?",
                String.class, paciente)).isEqualTo("09:00:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Solo con los datos obligatorios la fecha de inicio es hoy")
    void altaMinima(String rol) throws Exception {
        mockMvc.perform(alta(minimo(paciente, prestacion)).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaDesde").value("2026-12-01"))
                .andExpect(jsonPath("$.fechaHasta").value("2026-12-31"))
                .andExpect(jsonPath("$.profesionalId").value((Object) null))
                .andExpect(jsonPath("$.franjaDesde").value((Object) null))
                .andExpect(jsonPath("$.franjaHasta").value((Object) null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("El alta rechaza datos con formato inválido")
    void altaInvalida(String rol) throws Exception {
        String base = "\"pacienteId\":%d,\"prestacionId\":%d".formatted(paciente, prestacion);
        var invalidos = List.of(
                "{\"prestacionId\":%d,\"fechaHasta\":\"2026-12-31\"}".formatted(prestacion),
                "{\"pacienteId\":%d,\"fechaHasta\":\"2026-12-31\"}".formatted(paciente),
                "{" + base + "}",
                "{" + base + ",\"fechaHasta\":\"31/12/2026\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31T00:00:00\"}",
                "{" + base + ",\"fechaHasta\":20261231}",
                "{\"pacienteId\":\"%d\",\"prestacionId\":%d,\"fechaHasta\":\"2026-12-31\"}".formatted(paciente, prestacion),
                "{" + base + ",\"fechaDesde\":\"2026-12-10\",\"fechaHasta\":\"2026-12-09\"}",
                "{" + base + ",\"fechaHasta\":\"2026-11-30\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31\",\"franjaDesde\":\"09:00\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31\",\"franjaHasta\":\"09:00\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31\",\"franjaDesde\":\"9:00\",\"franjaHasta\":\"12:00\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31\",\"franjaDesde\":\"12:00\",\"franjaHasta\":\"12:00\"}",
                "{" + base + ",\"fechaHasta\":\"2026-12-31\",\"franjaDesde\":\"13:00\",\"franjaHasta\":\"12:00\"}",
                "{");
        for (String invalido : invalidos) {
            mockMvc.perform(alta(invalido).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidad(paciente)).isZero();
    }

    @Test
    @DisplayName("Un paciente sin email no se puede anotar")
    void pacienteSinEmail() throws Exception {
        Long sinEmail = DatosFicticios.paciente(jdbc, null);
        mockMvc.perform(alta(minimo(sinEmail, prestacion)).with(user(PERSONAL).roles("RECEPCION")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PACIENTE_SIN_EMAIL"));
        assertThat(cantidad(sinEmail)).isZero();
    }

    @Test
    @DisplayName("Paciente, prestación o profesional inexistente o inactivo se rechaza")
    void recursosInvalidos() throws Exception {
        Long pacienteInactivo = DatosFicticios.paciente(jdbc, "inactivo@correo.test", false);
        Long prestacionInactiva = DatosFicticios.prestacion(jdbc, "Inactiva lista", false);
        Long profesionalInactivo = DatosFicticios.profesional(jdbc, "MP-LISTA-INACTIVO", false);
        var casos = Map.of(
                minimo(-1L, prestacion), "PACIENTE_INEXISTENTE",
                minimo(pacienteInactivo, prestacion), "PACIENTE_INACTIVO",
                minimo(paciente, -1L), "PRESTACION_INEXISTENTE",
                minimo(paciente, prestacionInactiva), "PRESTACION_INACTIVA",
                conProfesional(-1L), "PROFESIONAL_INEXISTENTE",
                conProfesional(profesionalInactivo), "PROFESIONAL_INACTIVO");
        for (var caso : casos.entrySet()) {
            mockMvc.perform(alta(caso.getKey()).with(user(PERSONAL).roles("ADMIN")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value(caso.getValue()));
        }
        assertThat(jdbc.queryForObject("select count(*) from lista_espera", Long.class)).isZero();
    }

    // --- Duplicados ---

    @Test
    @DisplayName("Una segunda entrada activa para la misma prestación se rechaza")
    void duplicadoActivo() throws Exception {
        mockMvc.perform(alta(minimo(paciente, prestacion)).with(user(PERSONAL).roles("RECEPCION")))
                .andExpect(status().isCreated());
        mockMvc.perform(alta(minimo(paciente, prestacion)).with(user(PERSONAL).roles("RECEPCION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_ACTIVA_DUPLICADA"));
        assertThat(cantidad(paciente)).isEqualTo(1);
    }

    @Test
    @DisplayName("Otra prestación o una entrada ya cancelada no generan duplicado")
    void sinDuplicadoPorOtraPrestacionOCancelada() throws Exception {
        Long prestacionB = DatosFicticios.prestacion(jdbc, "Prestacion B");
        Long prestacionC = DatosFicticios.prestacion(jdbc, "Prestacion C");
        guardar(paciente, prestacion, "ACTIVA", "2026-12-31");
        guardar(paciente, prestacionB, "CANCELADA", "2026-12-31");
        for (Long otra : List.of(prestacionB, prestacionC)) {
            mockMvc.perform(alta(minimo(paciente, otra)).with(user(PERSONAL).roles("RECEPCION")))
                    .andExpect(status().isCreated());
        }
    }

    // --- Consulta ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION", "ODONTOLOGO"})
    @DisplayName("El listado filtra y ordena por antigüedad")
    void listadoFiltraYOrdena(String rol) throws Exception {
        Long otroPaciente = DatosFicticios.paciente(jdbc, "otro@correo.test");
        Long otraPrestacion = DatosFicticios.prestacion(jdbc, "Otra lista");
        Long primera = guardar(paciente, prestacion, "ACTIVA", "2026-12-31", "2026-11-01T10:00:00Z");
        Long segunda = guardar(otroPaciente, prestacion, "VENCIDA", "2026-11-15", "2026-11-02T10:00:00Z");
        Long tercera = guardar(paciente, otraPrestacion, "ACTIVA", "2026-12-31", "2026-11-03T10:00:00Z");

        var todos = ids(get(RUTA).param("pacienteId", "" + paciente).param("prestacionId", "" + prestacion), rol);
        assertThat(todos).containsExactly(primera);
        assertThat(ids(get(RUTA).param("estado", "ACTIVA").param("pacienteId", "" + paciente), rol))
                .containsExactly(primera, tercera);
        assertThat(ids(get(RUTA).param("prestacionId", "" + prestacion), rol)).containsExactly(primera, segunda);
        assertThat(ids(get(RUTA).param("estado", "VENCIDA").param("prestacionId", "" + prestacion), rol))
                .containsExactly(segunda);
        assertThat(ids(get(RUTA), rol)).containsSubsequence(primera, segunda, tercera);

        mockMvc.perform(get(RUTA).param("estado", "DESCONOCIDO").with(user(PERSONAL).roles(rol)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(DETALLE, tercera).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tercera))
                .andExpect(jsonPath("$.prestacionId").value(otraPrestacion));
    }

    @Test
    @DisplayName("Consultar una entrada inexistente devuelve 404")
    void detalleInexistente() throws Exception {
        mockMvc.perform(get(DETALLE, -1L).with(user(PERSONAL).roles("RECEPCION")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_NO_ENCONTRADA"));
    }

    // --- Baja ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Cancelar una entrada activa la conserva como cancelada")
    void cancelaActiva(String rol) throws Exception {
        Long entrada = guardar(paciente, prestacion, "ACTIVA", "2026-12-31");
        mockMvc.perform(delete(DETALLE, entrada).with(csrfValido()).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(estadoDe(entrada)).isEqualTo("CANCELADA");
        mockMvc.perform(alta(minimo(paciente, prestacion)).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated());
        mockMvc.perform(delete(DETALLE, -1L).with(csrfValido()).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_NO_ENCONTRADA"));
    }

    @Test
    @DisplayName("Una entrada que no está activa no se puede cancelar")
    void noCancelaNoActiva() throws Exception {
        for (String estado : List.of("VENCIDA", "CANCELADA", "ASIGNADA")) {
            Long entrada = guardar(paciente, DatosFicticios.prestacion(jdbc, "Final " + estado), estado, "2026-12-31");
            mockMvc.perform(delete(DETALLE, entrada).with(csrfValido()).with(user(PERSONAL).roles("RECEPCION")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ENTRADA_NO_ACTIVA"));
            assertThat(estadoDe(entrada)).isEqualTo(estado);
        }
    }

    // --- Acceso ---

    @Test
    @DisplayName("El odontólogo consulta pero no escribe")
    void odontologoSoloLee() throws Exception {
        Long entrada = guardar(paciente, prestacion, "ACTIVA", "2026-12-31");
        mockMvc.perform(get(RUTA).with(user(PERSONAL).roles("ODONTOLOGO"))).andExpect(status().isOk());
        mockMvc.perform(get(DETALLE, entrada).with(user(PERSONAL).roles("ODONTOLOGO"))).andExpect(status().isOk());
        var otraPrestacion = DatosFicticios.prestacion(jdbc, "Odontologo lista");
        for (var peticion : List.of(alta(minimo(paciente, otraPrestacion)), alta("{"),
                delete(DETALLE, entrada).with(csrfValido()), delete(DETALLE, "no-numerico").with(csrfValido()))) {
            mockMvc.perform(peticion.with(user(PERSONAL).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidad(paciente)).isEqualTo(1);
        assertThat(estadoDe(entrada)).isEqualTo("ACTIVA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Las escrituras requieren CSRF válido")
    void escriturasRequierenCsrf(String rol) throws Exception {
        Long entrada = guardar(paciente, prestacion, "ACTIVA", "2026-12-31");
        var otra = DatosFicticios.prestacion(jdbc, "Csrf lista");
        var sinToken = List.<MockHttpServletRequestBuilder>of(
                post(RUTA).contentType(MediaType.APPLICATION_JSON).content(minimo(paciente, otra)),
                delete(DETALLE, entrada));
        var conTokenIncorrecto = List.<MockHttpServletRequestBuilder>of(
                post(RUTA).contentType(MediaType.APPLICATION_JSON).content(minimo(paciente, otra))
                        .with(csrfConCabeceraIncorrecta()),
                delete(DETALLE, entrada).with(csrfConCabeceraIncorrecta()));
        for (var peticion : concatenar(sinToken, conTokenIncorrecto)) {
            mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidad(paciente)).isEqualTo(1);
        assertThat(estadoDe(entrada)).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("Sin sesión no se accede a la lista de espera")
    void exigeSesion() throws Exception {
        Long entrada = guardar(paciente, prestacion, "ACTIVA", "2026-12-31");
        var otra = DatosFicticios.prestacion(jdbc, "Sesion lista");
        for (var peticion : List.of(get(RUTA), get(DETALLE, entrada), alta(minimo(paciente, otra)),
                delete(DETALLE, entrada).with(csrfValido()))) {
            mockMvc.perform(peticion)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidad(paciente)).isEqualTo(1);
        assertThat(estadoDe(entrada)).isEqualTo("ACTIVA");
    }

    // --- Utilidades ---

    private List<Long> ids(MockHttpServletRequestBuilder peticion, String rol) throws Exception {
        String cuerpo = mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.<List<Number>>read(cuerpo, "$[*].id").stream()
                .map(Number::longValue).toList();
    }

    private MockHttpServletRequestBuilder alta(String cuerpo) throws Exception {
        return post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(csrfValido());
    }

    private String conProfesional(Long id) {
        return "{\"pacienteId\":%d,\"prestacionId\":%d,\"profesionalId\":%d,\"fechaHasta\":\"2026-12-31\"}"
                .formatted(paciente, prestacion, id);
    }

    private static String minimo(Long pacienteId, Long prestacionId) {
        return "{\"pacienteId\":%d,\"prestacionId\":%d,\"fechaHasta\":\"2026-12-31\"}".formatted(pacienteId, prestacionId);
    }

    private static String json(Map<String, Object> campos) {
        var partes = campos.entrySet().stream().map(e -> "\"" + e.getKey() + "\":"
                + (e.getValue() instanceof String s ? "\"" + s + "\"" : e.getValue())).toList();
        return "{" + String.join(",", partes) + "}";
    }

    private Long guardar(Long pac, Long pre, String estado, String hasta) {
        return guardar(pac, pre, estado, hasta, "2026-11-20T10:00:00Z");
    }

    private Long guardar(Long pac, Long pre, String estado, String hasta, String creadoEn) {
        return jdbc.queryForObject("""
                insert into lista_espera (paciente_id, prestacion_id, fecha_desde, fecha_hasta, estado,
                                          turno_asignado_id, creado_en)
                values (?, ?, date '2026-11-01', cast(? as date), ?, ?, cast(? as timestamptz)) returning id
                """, Long.class, pac, pre, hasta, estado, "ASIGNADA".equals(estado) ? 99L : null, creadoEn);
    }

    private Long cantidad(Long pac) {
        return jdbc.queryForObject("select count(*) from lista_espera where paciente_id = ?", Long.class, pac);
    }

    private String estadoDe(Long id) {
        return jdbc.queryForObject("select estado from lista_espera where id = ?", String.class, id);
    }

    private static <T> List<T> concatenar(List<T> a, List<T> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
