package ar.edu.utn.turnos.profesionales.horario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.LocalTime;
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
@Import(HorariosApiIT.VinculoFicticio.class)
class HorariosApiIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/profesionales/{id}/horarios";
    private static final String DETALLE = RUTA + "/{horarioId}";
    private static final String PERSONAL = "personal-ficticio";
    private static final String TITULAR = "odontologo-horarios-ficticio";
    private static final String MATRICULA = "MP-HORARIOS-API";
    private static final String ALTA = cuerpo(1, "09:00", "13:00");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private HorarioConsulta consulta;

    private Long profesional;
    private Long otroProfesional;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional(MATRICULA);
        otroProfesional = crearProfesional("MP-HORARIOS-OTRO");
    }

    // --- Alta y listado ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Administración y recepción crean y listan tramos del profesional solicitado")
    void creaYLista(String rol) throws Exception {
        guardarTramo(profesional, 2, "09:00", "12:00");
        guardarTramo(profesional, 1, "15:00", "19:00");
        guardarTramo(otroProfesional, 1, "08:00", "09:00");
        mockMvc.perform(alta(profesional, ALTA).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.profesionalId").value(profesional))
                .andExpect(jsonPath("$.diaSemana").value(1))
                .andExpect(jsonPath("$.horaInicio").value("09:00"))
                .andExpect(jsonPath("$.horaFin").value("13:00"));
        // La hora de pared se guarda sin desplazamiento de zona.
        assertThat(jdbc.queryForList(
                "select hora_inicio::text from horario_atencion where profesional_id = ? and dia_semana = 1 order by hora_inicio",
                String.class, profesional)).containsExactly("09:00:00", "15:00:00");
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].diaSemana").value(1))
                .andExpect(jsonPath("$[0].horaInicio").value("09:00"))
                .andExpect(jsonPath("$[1].horaInicio").value("15:00"))
                .andExpect(jsonPath("$[1].horaFin").value("19:00"))
                .andExpect(jsonPath("$[2].diaSemana").value(2))
                .andExpect(jsonPath("$[2].profesionalId").value(profesional));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Un profesional sin tramos devuelve un listado vacío")
    void listaVacia(String rol) throws Exception {
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("El alta rechaza días u horas inválidos")
    void altaInvalida(String rol) throws Exception {
        var invalidos = List.of(
                "{\"horaInicio\":\"09:00\",\"horaFin\":\"13:00\"}",
                "{\"diaSemana\":1,\"horaFin\":\"13:00\"}",
                "{\"diaSemana\":1,\"horaInicio\":\"09:00\"}",
                cuerpo(0, "09:00", "13:00"), cuerpo(8, "09:00", "13:00"),
                "{\"diaSemana\":\"1\",\"horaInicio\":\"09:00\",\"horaFin\":\"13:00\"}",
                "{\"diaSemana\":1.5,\"horaInicio\":\"09:00\",\"horaFin\":\"13:00\"}",
                cuerpo(1, "9:00", "13:00"), cuerpo(1, "09:00:00", "13:00"), cuerpo(1, "24:00", "13:00"),
                cuerpo(1, "09:60", "13:00"), cuerpo(1, "09:00", "abc"),
                "{\"diaSemana\":1,\"horaInicio\":900,\"horaFin\":\"13:00\"}",
                cuerpo(1, "13:00", "13:00"), cuerpo(1, "13:00", "09:00"), "{");
        for (String invalido : invalidos) {
            mockMvc.perform(alta(profesional, invalido).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(cantidadDeTramos(profesional)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Gestionar tramos de un profesional inexistente devuelve 404")
    void profesionalInexistente(String rol) throws Exception {
        for (var peticion : List.of(get(RUTA, -1L), alta(-1L, ALTA), reemplazo(-1L, -1L, ALTA),
                conCsrf(delete(DETALLE, -1L, -1L)))) {
            mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
        }
    }

    // --- No superposición ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Se aceptan varios tramos contiguos o separados en el mismo día")
    void aceptaContiguosYSeparados(String rol) throws Exception {
        guardarTramo(profesional, 1, "09:00", "13:00");
        for (String cuerpo : List.of(cuerpo(1, "13:00", "15:00"), cuerpo(1, "16:00", "20:00"))) {
            mockMvc.perform(alta(profesional, cuerpo).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isCreated());
        }
        assertThat(cantidadDeTramos(profesional)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Un tramo superpuesto en el mismo día se rechaza")
    void rechazaSuperpuesto(String rol) throws Exception {
        guardarTramo(profesional, 1, "09:00", "13:00");
        for (String cuerpo : List.of(cuerpo(1, "12:00", "14:00"), cuerpo(1, "10:00", "11:00"))) {
            mockMvc.perform(alta(profesional, cuerpo).with(user(PERSONAL).roles(rol)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value("HORARIO_SUPERPUESTO"));
        }
        assertThat(cantidadDeTramos(profesional)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Los tramos de otro día o de otro profesional no generan conflicto")
    void otroDiaUOtroProfesionalSinConflicto(String rol) throws Exception {
        guardarTramo(profesional, 1, "09:00", "13:00");
        mockMvc.perform(alta(profesional, cuerpo(2, "09:00", "13:00")).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated());
        mockMvc.perform(alta(otroProfesional, ALTA).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isCreated());
    }

    // --- Modificación y eliminación ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Modificar un tramo dentro de su propio rango se acepta")
    void modificaDentroDeSuRango(String rol) throws Exception {
        Long tramo = guardarTramo(profesional, 1, "09:00", "13:00");
        mockMvc.perform(reemplazo(profesional, tramo, cuerpo(1, "10:00", "14:00")).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tramo))
                .andExpect(jsonPath("$.horaInicio").value("10:00"))
                .andExpect(jsonPath("$.horaFin").value("14:00"));
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].horaInicio").value("10:00"))
                .andExpect(jsonPath("$[0].horaFin").value("14:00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Modificar un tramo para superponerlo con otro se rechaza")
    void modificarSuperpuestoSeRechaza(String rol) throws Exception {
        guardarTramo(profesional, 1, "09:00", "13:00");
        Long segundo = guardarTramo(profesional, 1, "14:00", "18:00");
        mockMvc.perform(reemplazo(profesional, segundo, cuerpo(1, "12:00", "18:00")).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HORARIO_SUPERPUESTO"));
        assertThat(horasDe(segundo)).containsExactly("14:00:00", "18:00:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Eliminar un tramo lo retira del listado y de la consulta de contención")
    void eliminaTramo(String rol) throws Exception {
        Long tramo = guardarTramo(profesional, 1, "09:00", "13:00");
        // 2030-06-10 es lunes.
        var candidato = new Intervalo(OffsetDateTime.parse("2030-06-10T10:00:00-03:00").toInstant(),
                OffsetDateTime.parse("2030-06-10T10:30:00-03:00").toInstant());
        assertThat(consulta.contiene(profesional, candidato)).isTrue();

        mockMvc.perform(conCsrf(delete(DETALLE, profesional, tramo)).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mockMvc.perform(get(RUTA, profesional).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(consulta.contiene(profesional, candidato)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Un tramo ausente o de otro profesional no se puede modificar ni eliminar")
    void noModificaNiEliminaAjenoOAusente(String rol) throws Exception {
        Long propio = guardarTramo(profesional, 1, "09:00", "13:00");
        Long ajeno = guardarTramo(otroProfesional, 1, "09:00", "13:00");
        for (Long id : List.of(ajeno, -1L)) {
            for (var peticion : List.of(reemplazo(profesional, id, cuerpo(3, "08:00", "09:00")),
                    conCsrf(delete(DETALLE, profesional, id)))) {
                mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.codigo").value("HORARIO_NO_ENCONTRADO"));
            }
        }
        assertThat(horasDe(propio)).containsExactly("09:00:00", "13:00:00");
        assertThat(horasDe(ajeno)).containsExactly("09:00:00", "13:00:00");
    }

    // --- Acceso ---

    @Test
    @DisplayName("El odontólogo vinculado lista sus tramos pero no los ajenos")
    void leeSoloTramosPropios() throws Exception {
        Long propio = guardarTramo(profesional, 1, "09:00", "13:00");
        guardarTramo(otroProfesional, 1, "09:00", "13:00");
        mockMvc.perform(get(RUTA, profesional).with(user(TITULAR).roles("ODONTOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propio));
        for (Long id : List.of(otroProfesional, -1L)) {
            mockMvc.perform(get(RUTA, id).with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.horaInicio").doesNotExist());
        }
    }

    @Test
    @DisplayName("El odontólogo sin vínculo no puede listar tramos")
    void noLeeSinVinculo() throws Exception {
        guardarTramo(profesional, 1, "09:00", "13:00");
        mockMvc.perform(get(RUTA, profesional).with(user("sin-vinculo").roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.horaInicio").doesNotExist());
    }

    @Test
    @DisplayName("El odontólogo no puede crear, modificar ni eliminar tramos")
    void odontologoNoEscribe() throws Exception {
        Long tramo = guardarTramo(profesional, 1, "09:00", "13:00");
        for (var peticion : List.of(alta(profesional, cuerpo(2, "09:00", "13:00")), alta(profesional, "{"),
                reemplazo(profesional, tramo, cuerpo(1, "10:00", "14:00")), reemplazo(profesional, tramo, "{"),
                conCsrf(delete(DETALLE, profesional, tramo)),
                conCsrf(delete(DETALLE, profesional, "no-numerico")))) {
            mockMvc.perform(peticion.with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            verificarTramoIntacto(tramo);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Las escrituras de tramos requieren CSRF válido")
    void escriturasRequierenCsrf(String rol) throws Exception {
        Long tramo = guardarTramo(profesional, 1, "09:00", "13:00");
        for (boolean invalido : List.of(false, true)) {
            var alta = post(RUTA, profesional).contentType(MediaType.APPLICATION_JSON).content(cuerpo(2, "09:00", "13:00"));
            var cambio = put(DETALLE, profesional, tramo).contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo(1, "10:00", "14:00"));
            for (var peticion : List.of(alta, cambio, delete(DETALLE, profesional, tramo))) {
                if (invalido) {
                    peticion.cookie(cookieCsrf()).header("X-XSRF-TOKEN", "otro");
                }
                mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
                verificarTramoIntacto(tramo);
            }
        }
    }

    @Test
    @DisplayName("Consultar o escribir tramos sin sesión devuelve 401")
    void exigeSesion() throws Exception {
        Long tramo = guardarTramo(profesional, 1, "09:00", "13:00");
        for (var peticion : List.of(get(RUTA, profesional), alta(profesional, cuerpo(2, "09:00", "13:00")),
                reemplazo(profesional, tramo, cuerpo(1, "10:00", "14:00")),
                conCsrf(delete(DETALLE, profesional, tramo)))) {
            mockMvc.perform(peticion).andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
            verificarTramoIntacto(tramo);
        }
    }

    // --- Utilidades ---

    private void verificarTramoIntacto(Long tramo) {
        assertThat(cantidadDeTramos(profesional)).isEqualTo(1);
        assertThat(horasDe(tramo)).containsExactly("09:00:00", "13:00:00");
    }

    private List<String> horasDe(Long tramo) {
        return jdbc.queryForObject("select hora_inicio::text, hora_fin::text from horario_atencion where id = ?",
                (fila, n) -> List.of(fila.getString(1), fila.getString(2)), tramo);
    }

    private MockHttpServletRequestBuilder alta(Long id, String cuerpo) throws Exception {
        return conCsrf(post(RUTA, id).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private MockHttpServletRequestBuilder reemplazo(Long id, Long horarioId, String cuerpo) throws Exception {
        return conCsrf(put(DETALLE, id, horarioId).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
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

    private Long guardarTramo(Long id, int dia, String inicio, String fin) {
        return jdbc.queryForObject("""
                insert into horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin)
                values (?, ?, ?, ?) returning id
                """, Long.class, id, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private Long cantidadDeTramos(Long id) {
        return jdbc.queryForObject("select count(*) from horario_atencion where profesional_id = ?", Long.class, id);
    }

    private static String cuerpo(int dia, String inicio, String fin) {
        return """
                {"diaSemana":%d,"horaInicio":"%s","horaFin":"%s"}
                """.formatted(dia, inicio, fin);
    }

    @TestConfiguration
    static class VinculoFicticio {
        @Bean
        ProfesionalVinculadoConsulta profesionalVinculadoHorarios(JdbcTemplate jdbc) {
            return identidad -> TITULAR.equals(identidad)
                    ? jdbc.queryForList("select id from profesional where matricula = ?", Long.class, MATRICULA)
                            .stream().findFirst()
                    : Optional.empty();
        }
    }
}
