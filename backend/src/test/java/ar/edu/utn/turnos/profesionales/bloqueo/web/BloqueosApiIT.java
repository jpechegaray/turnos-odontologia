package ar.edu.utn.turnos.profesionales.bloqueo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import(BloqueosApiIT.VinculoFicticio.class)
class BloqueosApiIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/profesionales/{id}/bloqueos";
    private static final String DETALLE = RUTA + "/{bloqueoId}";
    private static final String PERSONAL = "personal-ficticio";
    private static final String TITULAR = "odontologo-ficticio";
    private static final String MATRICULA = "MP-BLOQUEOS-API";
    private static final String INICIO = "2030-06-10T10:00:00-03:00";
    private static final String FIN = "2030-06-10T11:00:00-03:00";
    private static final String ALTA = cuerpo(INICIO, FIN, "Congreso");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BloqueoConsulta consulta;

    private Long profesional;
    private Long otroProfesional;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional(MATRICULA);
        otroProfesional = crearProfesional("MP-BLOQUEOS-OTRO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Administración y recepción crean y listan bloqueos del profesional solicitado")
    void creaYLista(String rol) throws Exception {
        var diez = OffsetDateTime.parse(INICIO);
        guardarBloqueo(profesional, diez.plusHours(2), "Último");
        guardarBloqueo(profesional, diez, "Empate anterior");
        guardarBloqueo(otroProfesional, diez.minusHours(1), "Ajeno");
        mockMvc.perform(alta(profesional, ALTA).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.profesionalId").value(profesional))
                .andExpect(jsonPath("$.inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2030-06-10T14:00:00Z"))
                .andExpect(jsonPath("$.motivo").value("Congreso"));
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].motivo").value("Empate anterior"))
                .andExpect(jsonPath("$[1].motivo").value("Congreso"))
                .andExpect(jsonPath("$[1].inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$[2].motivo").value("Último"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Un profesional sin bloqueos devuelve un listado vacío")
    void listaVacia(String rol) throws Exception {
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("El alta rechaza fechas o motivos inválidos")
    void altaInvalida(String rol) throws Exception {
        var invalidos = List.of(cuerpo(null, FIN, "Congreso"), cuerpo(INICIO, null, "Congreso"),
                cuerpo("2030-06-10T10:00:00", FIN, "Congreso"),
                cuerpo("2030-06-10T10:00:00.0000000-03:00", FIN, "Congreso"),
                cuerpo(INICIO, INICIO, "Congreso"), cuerpo(FIN, INICIO, "Congreso"),
                cuerpo(INICIO, FIN, null), cuerpo(INICIO, FIN, " "), cuerpo(INICIO, FIN, "a".repeat(256)),
                ALTA.replace("\"Congreso\"", "123"));
        for (String invalido : invalidos) {
            mockMvc.perform(alta(profesional, invalido).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidadDeBloqueos(profesional)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Gestionar bloqueos de un profesional inexistente devuelve 404")
    void profesionalInexistente(String rol) throws Exception {
        for (var peticion : List.of(get(RUTA, -1L), alta(-1L, ALTA), conCsrf(delete(DETALLE, -1L, -1L)))) {
            mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Eliminar un bloqueo lo retira del listado y de la consulta de solapamiento")
    void eliminaBloqueo(String rol) throws Exception {
        var inicio = OffsetDateTime.parse(INICIO);
        var intervalo = new Intervalo(inicio.toInstant(), inicio.plusHours(1).toInstant());
        Long bloqueo = guardarBloqueo(profesional, inicio, "Congreso");
        assertThat(consulta.solapa(profesional, intervalo)).isTrue();

        mockMvc.perform(conCsrf(delete(DETALLE, profesional, bloqueo)).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(consulta.solapa(profesional, intervalo)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Un bloqueo ausente o de otro profesional no se puede eliminar")
    void noEliminaBloqueoAjenoOAusente(String rol) throws Exception {
        var inicio = OffsetDateTime.parse(INICIO);
        guardarBloqueo(profesional, inicio, "Propio");
        Long ajeno = guardarBloqueo(otroProfesional, inicio, "Ajeno");
        for (Long id : List.of(ajeno, -1L)) {
            mockMvc.perform(conCsrf(delete(DETALLE, profesional, id)).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("BLOQUEO_NO_ENCONTRADO"));
        }
        assertThat(cantidadDeBloqueos(profesional)).isEqualTo(1);
        assertThat(cantidadDeBloqueos(otroProfesional)).isEqualTo(1);
    }

    @Test
    @DisplayName("El odontólogo vinculado lista sus bloqueos pero no los ajenos")
    void leeSoloBloqueosPropios() throws Exception {
        var inicio = OffsetDateTime.parse(INICIO);
        Long propio = guardarBloqueo(profesional, inicio, "Propio");
        guardarBloqueo(otroProfesional, inicio, "Ajeno");
        mockMvc.perform(get(RUTA, profesional).with(user(TITULAR).roles("ODONTOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propio));
        for (Long id : List.of(otroProfesional, -1L)) {
            mockMvc.perform(get(RUTA, id).with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.motivo").doesNotExist());
        }
    }

    @Test
    @DisplayName("El odontólogo sin vínculo no puede listar bloqueos")
    void noLeeSinVinculo() throws Exception {
        guardarBloqueo(profesional, OffsetDateTime.parse(INICIO), "Congreso");
        mockMvc.perform(get(RUTA, profesional).with(user("sin-vinculo").roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.motivo").doesNotExist());
    }

    @Test
    @DisplayName("El odontólogo no puede crear ni eliminar bloqueos")
    void odontologoNoEscribe() throws Exception {
        Long bloqueo = guardarBloqueo(profesional, OffsetDateTime.parse(INICIO), "Original");
        for (var peticion : List.of(alta(profesional, ALTA), alta(profesional, "{"),
                conCsrf(delete(DETALLE, profesional, bloqueo)),
                conCsrf(delete(DETALLE, profesional, "no-numerico")))) {
            mockMvc.perform(peticion.with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            verificarBloqueoIntacto(bloqueo);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Las escrituras de bloqueos requieren CSRF válido")
    void escriturasRequierenCsrf(String rol) throws Exception {
        Long bloqueo = guardarBloqueo(profesional, OffsetDateTime.parse(INICIO), "Original");
        for (boolean invalido : List.of(false, true)) {
            var alta = post(RUTA, profesional).contentType(MediaType.APPLICATION_JSON).content(ALTA);
            for (var peticion : List.of(alta, delete(DETALLE, profesional, bloqueo))) {
                if (invalido) {
                    peticion.cookie(cookieCsrf()).header("X-XSRF-TOKEN", "otro");
                }
                mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
                verificarBloqueoIntacto(bloqueo);
            }
        }
    }

    @Test
    @DisplayName("Consultar o escribir bloqueos sin sesión devuelve 401")
    void exigeSesion() throws Exception {
        Long bloqueo = guardarBloqueo(profesional, OffsetDateTime.parse(INICIO), "Original");
        for (var peticion : List.of(get(RUTA, profesional), alta(profesional, ALTA),
                conCsrf(delete(DETALLE, profesional, bloqueo)))) {
            mockMvc.perform(peticion).andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            verificarBloqueoIntacto(bloqueo);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Crear un bloqueo superpuesto con turnos activos devuelve 409 con la lista de conflictos")
    void rechazaBloqueoSuperpuestoConTurnosActivos(String rol) throws Exception {
        // Inserta primero el de 10:30..11:00 y luego 10:00..10:30 para comprobar orden inicio ASC, id ASC.
        Long confirmado = insertarTurno("2030-06-10T10:30:00-03:00", "2030-06-10T11:00:00-03:00", "CONFIRMADO");
        Long programado = insertarTurno("2030-06-10T10:00:00-03:00", "2030-06-10T10:30:00-03:00", "PROGRAMADO");

        var solapado = cuerpo("2030-06-10T10:15:00-03:00", "2030-06-10T10:45:00-03:00", "Mantenimiento");
        mockMvc.perform(alta(profesional, solapado).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-17"))
                .andExpect(jsonPath("$.turnos.length()").value(2))
                .andExpect(jsonPath("$.turnos[0].id").value(programado))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$.turnos[0].fin").value("2030-06-10T13:30:00Z"))
                .andExpect(jsonPath("$.turnos[0].estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.turnos[0].pacienteId").doesNotExist())
                .andExpect(jsonPath("$.turnos[0].paciente").doesNotExist())
                .andExpect(jsonPath("$.turnos[1].id").value(confirmado))
                .andExpect(jsonPath("$.turnos[1].inicio").value("2030-06-10T13:30:00Z"))
                .andExpect(jsonPath("$.turnos[1].fin").value("2030-06-10T14:00:00Z"))
                .andExpect(jsonPath("$.turnos[1].estado").value("CONFIRMADO"));
        assertThat(cantidadDeBloqueos(profesional)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Crear un bloqueo contiguo a turnos activos se acepta")
    void aceptaBloqueoContiguoATurnosActivos(String rol) throws Exception {
        insertarTurno("2030-06-10T09:30:00-03:00", "2030-06-10T10:00:00-03:00", "PROGRAMADO");
        insertarTurno("2030-06-10T11:00:00-03:00", "2030-06-10T11:30:00-03:00", "CONFIRMADO");

        mockMvc.perform(alta(profesional, ALTA).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2030-06-10T14:00:00Z"));
        assertThat(cantidadDeBloqueos(profesional)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Cancelar el turno activo permite crear el bloqueo antes rechazado")
    void cancelarTurnoPermiteCrearBloqueo(String rol) throws Exception {
        Long turnoId = insertarTurno("2030-06-10T10:00:00-03:00", "2030-06-10T10:30:00-03:00", "PROGRAMADO");

        var bloqueoEnHorario = cuerpo("2030-06-10T10:00:00-03:00", "2030-06-10T10:30:00-03:00", "Congreso");
        mockMvc.perform(alta(profesional, bloqueoEnHorario).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-17"));

        mockMvc.perform(conCsrf(post("/api/turnos/{id}/cancelar", turnoId)).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk());

        mockMvc.perform(alta(profesional, bloqueoEnHorario).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2030-06-10T13:30:00Z"));
    }

    private Long insertarTurno(String inicio, String fin, String estado) {
        Long paciente = DatosAgenda.paciente(jdbc, true);
        Long sillon = DatosAgenda.sillon(jdbc, true);
        Long prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio, fin, estado);
    }

    private void verificarBloqueoIntacto(Long bloqueo) {
        assertThat(cantidadDeBloqueos(profesional)).isEqualTo(1);
        assertThat(jdbc.queryForList("select motivo from bloqueo where id = ?", String.class, bloqueo))
                .containsExactly("Original");
    }

    private MockHttpServletRequestBuilder alta(Long id, String cuerpo) throws Exception {
        return conCsrf(post(RUTA, id).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private MockHttpServletRequestBuilder conCsrf(MockHttpServletRequestBuilder peticion) throws Exception {
        var token = cookieCsrf();
        return peticion.cookie(token).header("X-XSRF-TOKEN", token.getValue());
    }

    private jakarta.servlet.http.Cookie cookieCsrf() throws Exception {
        var token = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        return token;
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private Long guardarBloqueo(Long id, OffsetDateTime inicio, String motivo) {
        return jdbc.queryForObject("""
                insert into bloqueo (profesional_id, inicio, fin, motivo) values (?, ?, ?, ?) returning id
                """, Long.class, id, inicio, inicio.plusHours(1), motivo);
    }

    private Long cantidadDeBloqueos(Long id) {
        return jdbc.queryForObject("select count(*) from bloqueo where profesional_id = ?", Long.class, id);
    }

    private static String cuerpo(String inicio, String fin, String motivo) {
        return """
                {"inicio":%s,"fin":%s,"motivo":%s}
                """.formatted(valorJson(inicio), valorJson(fin), valorJson(motivo));
    }

    private static String valorJson(String texto) {
        return texto == null ? "null" : "\"" + texto + "\"";
    }

    @TestConfiguration
    static class VinculoFicticio {
        @Bean
        ProfesionalVinculadoConsulta profesionalVinculado(JdbcTemplate jdbc) {
            return identidad -> TITULAR.equals(identidad)
                    ? jdbc.queryForList("select id from profesional where matricula = ?", Long.class, MATRICULA)
                            .stream().findFirst()
                    : Optional.empty();
        }
    }
}
