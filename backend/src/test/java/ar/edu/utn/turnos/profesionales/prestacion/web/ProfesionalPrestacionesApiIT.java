package ar.edu.utn.turnos.profesionales.prestacion.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
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
@Import(ProfesionalPrestacionesApiIT.VinculoFicticio.class)
class ProfesionalPrestacionesApiIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/profesionales/{id}/prestaciones";
    private static final String ADMIN = "admin-ficticio";
    private static final String TITULAR = "odontologo-ficticio";
    private static final String MATRICULA = "MP-PRESTACIONES-API";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProfesionalRealizaPrestacion puertoRealiza;

    private Long profesional;
    private Long otroProfesional;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional(MATRICULA, true);
        otroProfesional = crearProfesional("MP-PRESTACIONES-OTRO", true);
    }

    @Test
    @DisplayName("Se listan las prestaciones del profesional ordenadas por nombre")
    void listaPrestacionesOrdenadasPorNombre() throws Exception {
        Long limpieza = crearPrestacion("Limpieza", 45, true);
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long extraccion = crearPrestacion("Extracción", 60, true);
        asociar(profesional, limpieza);
        asociar(profesional, consulta);
        asociar(otroProfesional, extraccion);

        mockMvc.perform(get(RUTA, profesional).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"))
                .andExpect(jsonPath("$[0].duracionDefaultMin").value(30))
                .andExpect(jsonPath("$[0].activa").value(true))
                .andExpect(jsonPath("$[1].id").value(limpieza))
                .andExpect(jsonPath("$[1].nombre").value("Limpieza"))
                .andExpect(jsonPath("$[1].duracionDefaultMin").value(45))
                .andExpect(jsonPath("$[1].activa").value(true));
    }

    @Test
    @DisplayName("Un profesional sin prestaciones devuelve una lista vacía")
    void listaVaciaCuandoNoTienePrestaciones() throws Exception {
        mockMvc.perform(get(RUTA, profesional).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Una prestación asociada que se dio de baja se muestra inactiva")
    void muestraPrestacionInactivaAsociada() throws Exception {
        Long blanqueamiento = crearPrestacion("Blanqueamiento", 60, true);
        asociar(profesional, blanqueamiento);
        jdbc.update("update prestacion set activa = false where id = ?", blanqueamiento);

        mockMvc.perform(get(RUTA, profesional).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(blanqueamiento))
                .andExpect(jsonPath("$[0].nombre").value("Blanqueamiento"))
                .andExpect(jsonPath("$[0].activa").value(false));
    }

    @Test
    @DisplayName("Consultar las prestaciones de un profesional inexistente responde 404")
    void consultarProfesionalInexistenteResponde404() throws Exception {
        mockMvc.perform(get(RUTA, -1L).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("El administrador asocia prestaciones a un profesional")
    void administradorAsociaPrestaciones() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long limpieza = crearPrestacion("Limpieza", 45, true);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(limpieza, consulta))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"))
                .andExpect(jsonPath("$[1].id").value(limpieza))
                .andExpect(jsonPath("$[1].nombre").value("Limpieza"));

        mockMvc.perform(get(RUTA, profesional).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[1].id").value(limpieza));
    }

    @Test
    @DisplayName("El reemplazo desasocia las prestaciones que no se envían")
    void reemplazoDesasociaPrestacionesNoEnviadas() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long limpieza = crearPrestacion("Limpieza", 45, true);
        Long extraccion = crearPrestacion("Extracción", 60, true);
        asociar(profesional, consulta);
        asociar(profesional, limpieza);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(limpieza, extraccion))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(extraccion))
                .andExpect(jsonPath("$[0].nombre").value("Extracción"))
                .andExpect(jsonPath("$[1].id").value(limpieza))
                .andExpect(jsonPath("$[1].nombre").value("Limpieza"));

        assertThat(puertoRealiza.realiza(profesional, consulta)).isFalse();
        assertThat(puertoRealiza.realiza(profesional, limpieza)).isTrue();
        assertThat(puertoRealiza.realiza(profesional, extraccion)).isTrue();
    }

    @Test
    @DisplayName("Una lista vacía deja al profesional sin prestaciones")
    void listaVaciaDejaProfesionalSinPrestaciones() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        mockMvc.perform(reemplazo(profesional, "{\"prestacionIds\":[]}")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        assertThat(puertoRealiza.realiza(profesional, consulta)).isFalse();
    }

    @Test
    @DisplayName("Los ids repetidos se asocian una sola vez")
    void idsRepetidosSeAsocianUnaSolaVez() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(consulta, consulta))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"));
    }

    @Test
    @DisplayName("Reemplazar las prestaciones de un profesional inexistente responde 404")
    void reemplazarProfesionalInexistenteResponde404() throws Exception {
        mockMvc.perform(reemplazo(-1L, "{\"prestacionIds\":[]}")
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Las prestaciones de un profesional inactivo se pueden consultar y reemplazar")
    void profesionalInactivoSePuedeConsultarYReemplazar() throws Exception {
        Long inactivo = crearProfesional("MP-INACTIVO-PREST", false);
        Long consulta = crearPrestacion("Consulta", 30, true);

        mockMvc.perform(reemplazo(inactivo, cuerpoIds(consulta))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"));

        mockMvc.perform(get(RUTA, inactivo).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"));
    }

    @Test
    @DisplayName("Un cuerpo inválido responde 400 sin cambios")
    void cuerpoInvalidoResponde400SinCambios() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        for (String invalido : List.of("{}", "{\"prestacionIds\":null}", "{\"prestacionIds\":[null]}")) {
            mockMvc.perform(reemplazo(profesional, invalido).with(user(ADMIN).roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verificarUnicaPrestacionAsociada(profesional, consulta);
    }

    @Test
    @DisplayName("Asociar una prestación inexistente responde 422")
    void asociarPrestacionInexistenteResponde422() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(consulta, -1L))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("PRESTACION_INEXISTENTE"));

        verificarUnicaPrestacionAsociada(profesional, consulta);
    }

    @Test
    @DisplayName("Asociar una prestación inactiva responde 422")
    void asociarPrestacionInactivaResponde422() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long blanqueamiento = crearPrestacion("Blanqueamiento", 60, false);
        asociar(profesional, consulta);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(consulta, blanqueamiento))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("PRESTACION_INACTIVA"));

        verificarUnicaPrestacionAsociada(profesional, consulta);
    }

    @Test
    @DisplayName("Se puede conservar una prestación ya asociada que se dio de baja")
    void conservaPrestacionYaAsociadaQueSeDioDeBaja() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long blanqueamiento = crearPrestacion("Blanqueamiento", 60, true);
        asociar(profesional, consulta);
        asociar(profesional, blanqueamiento);
        jdbc.update("update prestacion set activa = false where id = ?", blanqueamiento);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(consulta, blanqueamiento))
                        .with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(blanqueamiento))
                .andExpect(jsonPath("$[0].nombre").value("Blanqueamiento"))
                .andExpect(jsonPath("$[0].activa").value(false))
                .andExpect(jsonPath("$[1].id").value(consulta))
                .andExpect(jsonPath("$[1].nombre").value("Consulta"))
                .andExpect(jsonPath("$[1].activa").value(true));
    }

    @Test
    @DisplayName("Recepción consulta las prestaciones de cualquier profesional")
    void recepcionConsultaPrestacionesDeCualquierProfesional() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        mockMvc.perform(get(RUTA, profesional).with(user("recepcion-ficticia").roles("RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"));
    }

    @Test
    @DisplayName("Un odontólogo vinculado consulta solo sus propias prestaciones")
    void odontologoVinculadoConsultaSoloPropias() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long limpieza = crearPrestacion("Limpieza", 45, true);
        asociar(profesional, consulta);
        asociar(otroProfesional, limpieza);

        mockMvc.perform(get(RUTA, profesional).with(user(TITULAR).roles("ODONTOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"));

        for (Long id : List.of(otroProfesional, -1L)) {
            mockMvc.perform(get(RUTA, id).with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.nombre").doesNotExist());
        }
    }

    @Test
    @DisplayName("Un odontólogo sin vínculo no puede consultar prestaciones de profesionales")
    void odontologoSinVinculoNoPuedeConsultar() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        mockMvc.perform(get(RUTA, profesional).with(user("sin-vinculo").roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPCION", "ODONTOLOGO"})
    @DisplayName("Recepción y odontología no pueden reemplazar las prestaciones")
    void recepcionYOdontologiaNoPuedenReemplazar(String rol) throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        Long limpieza = crearPrestacion("Limpieza", 45, true);
        asociar(profesional, consulta);

        mockMvc.perform(reemplazo(profesional, cuerpoIds(limpieza))
                        .with(user(TITULAR).roles(rol)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verificarUnicaPrestacionAsociada(profesional, consulta);
    }

    @Test
    @DisplayName("Sin sesión las prestaciones de un profesional responden 401")
    void sinSesionResponden401() throws Exception {
        Long consulta = crearPrestacion("Consulta", 30, true);
        asociar(profesional, consulta);

        for (var peticion : List.of(get(RUTA, profesional), reemplazo(profesional, "{\"prestacionIds\":[]}"))) {
            mockMvc.perform(peticion)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verificarUnicaPrestacionAsociada(profesional, consulta);
    }

    private void verificarUnicaPrestacionAsociada(Long idProfesional, Long idPrestacion) throws Exception {
        mockMvc.perform(get(RUTA, idProfesional).with(user(ADMIN).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idPrestacion));
    }

    private MockHttpServletRequestBuilder reemplazo(Long id, String cuerpo) throws Exception {
        return conCsrf(put(RUTA, id).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
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

    private static String cuerpoIds(Long... ids) {
        var unidos = java.util.Arrays.stream(ids).map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"prestacionIds\":[" + unidos + "]}";
    }

    private void asociar(Long idProfesional, Long idPrestacion) {
        jdbc.update("insert into profesional_prestacion (profesional_id, prestacion_id) values (?, ?)",
                idProfesional, idPrestacion);
    }

    private Long crearProfesional(String matricula, boolean activo) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, ?) returning id
                """, Long.class, matricula, activo);
    }

    private Long crearPrestacion(String nombre, int duracion, boolean activa) {
        return jdbc.queryForObject("""
                insert into prestacion (nombre, nombre_clave, duracion_default_min, activa)
                values (?, ?, ?, ?) returning id
                """, Long.class, nombre, nombre.toLowerCase(), duracion, activa);
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
