package ar.edu.utn.turnos.prestaciones.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Escenarios de la spec {@code catalogo-prestaciones}: cada {@code @DisplayName} es el nombre exacto del escenario. */
@Transactional
class CatalogoPrestacionesIT extends PruebaIntegracionBase {

    private static final String URL = "/api/prestaciones";
    private static final long INEXISTENTE = 999_999L;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ConsultaPrestaciones consulta;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock reloj;

    // --- Alta de prestación ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El administrador da de alta una prestación válida")
    void altaValida() throws Exception {
        alta("Limpieza", 45)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Limpieza"))
                .andExpect(jsonPath("$.duracionDefaultMin").value(45))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Un nombre vacío responde 400")
    void nombreVacio() throws Exception {
        mockMvc.perform(post(URL).with(csrfValido()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo").value(hasItem("duracionDefaultMin")));
    }

    // --- Duración por defecto válida ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Una duración no múltiplo de 5 responde 422")
    void duracionNoMultiplo() throws Exception {
        esperarRn09(alta("Consulta", 7));
        mockMvc.perform(get(URL)).andExpect(jsonPath("$[*].nombre").value(not(hasItem("Consulta"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Una duración mayor a 480 responde 422")
    void duracionMayor() throws Exception {
        esperarRn09(alta("Consulta", 481));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Una duración menor a 5 responde 422")
    void duracionMenor() throws Exception {
        esperarRn09(alta("Consulta", 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Las duraciones límite 5 y 480 se aceptan")
    void duracionesLimite() throws Exception {
        alta("Control breve", 5).andExpect(status().isCreated());
        alta("Cirugía extensa", 480).andExpect(status().isCreated());
    }

    // --- Nombre de prestación único ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Un nombre repetido responde 409")
    void nombreRepetido() throws Exception {
        alta("Limpieza", 45).andExpect(status().isCreated());

        alta(" limpieza ", 30)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRESTACION-DUPLICADA"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Modificar una prestación conservando su propio nombre se acepta")
    void modificarConservandoNombre() throws Exception {
        long id = idDe(alta("Limpieza", 45));

        modificar(id, "Limpieza", 60, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracionDefaultMin").value(60));
    }

    // --- Modificación de prestación ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El administrador modifica una prestación")
    void modifica() throws Exception {
        long id = idDe(alta("Consulta", 30));

        modificar(id, "Consulta inicial", 40, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Consulta inicial"))
                .andExpect(jsonPath("$.duracionDefaultMin").value(40))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El administrador reactiva una prestación dada de baja")
    void reactiva() throws Exception {
        long id = idDe(alta("Blanqueamiento", 60));
        mockMvc.perform(delete(URL + "/" + id).with(csrfValido())).andExpect(status().isNoContent());

        modificar(id, "Blanqueamiento", 60, true).andExpect(status().isOk());

        mockMvc.perform(get(URL).param("soloActivas", "true"))
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Blanqueamiento")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Modificar una prestación inexistente responde 404")
    void modificarInexistente() throws Exception {
        esperarNoEncontrada(modificar(INEXISTENTE, "Consulta", 30, true));
    }

    // --- Baja lógica de prestación ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("La baja deja la prestación inactiva pero consultable")
    void bajaLogica() throws Exception {
        long id = idDe(alta("Blanqueamiento", 60));

        mockMvc.perform(delete(URL + "/" + id).with(csrfValido())).andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Blanqueamiento"))
                .andExpect(jsonPath("$.activa").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Repetir la baja responde 204")
    void repetirBaja() throws Exception {
        long id = idDe(alta("Blanqueamiento", 60));
        mockMvc.perform(delete(URL + "/" + id).with(csrfValido())).andExpect(status().isNoContent());

        mockMvc.perform(delete(URL + "/" + id).with(csrfValido())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Dar de baja una prestación inexistente responde 404")
    void bajaInexistente() throws Exception {
        esperarNoEncontrada(mockMvc.perform(delete(URL + "/" + INEXISTENTE).with(csrfValido())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Dar de baja o desactivar una prestación con turnos futuros activos devuelve 409 con la lista de conflictos")
    void desactivarConTurnosFuturosDevuelve409() throws Exception {
        long id = idDe(alta("Endodoncia", 60));
        Long paciente = DatosAgenda.paciente(jdbc, true);
        Long profesional = DatosAgenda.profesional(jdbc, true);
        Long sillon = DatosAgenda.sillon(jdbc, true);
        Instant base = Instant.now(reloj).plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.SECONDS);
        Instant inicio2 = base.plus(Duration.ofHours(1));
        Instant fin2 = inicio2.plus(Duration.ofMinutes(60));
        Instant inicio1 = base;
        Instant fin1 = inicio1.plus(Duration.ofMinutes(60));
        Long segundo = DatosAgenda.turno(jdbc, paciente, profesional, sillon, id, inicio2.toString(),
                fin2.toString(), "CONFIRMADO");
        Long primero = DatosAgenda.turno(jdbc, paciente, profesional, sillon, id, inicio1.toString(),
                fin1.toString(), "PROGRAMADO");

        mockMvc.perform(delete(URL + "/" + id).with(csrfValido()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-37"))
                .andExpect(jsonPath("$.recurso").value("PRESTACION"))
                .andExpect(jsonPath("$.turnos.length()").value(2))
                .andExpect(jsonPath("$.turnos[0].id").value(primero))
                .andExpect(jsonPath("$.turnos[0].inicio").value(inicio1.toString()))
                .andExpect(jsonPath("$.turnos[0].fin").value(fin1.toString()))
                .andExpect(jsonPath("$.turnos[0].estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.turnos[0].pacienteId").doesNotExist())
                .andExpect(jsonPath("$.turnos[1].id").value(segundo))
                .andExpect(jsonPath("$.turnos[1].estado").value("CONFIRMADO"));

        modificar(id, "Endodoncia", 60, false)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-37"))
                .andExpect(jsonPath("$.recurso").value("PRESTACION"))
                .andExpect(jsonPath("$.turnos.length()").value(2));

        assertThat(consulta.buscarPorId(id)).hasValueSatisfying(p -> assertThat(p.activa()).isTrue());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cancelar los turnos futuros permite dar de baja la prestación")
    void cancelarTurnosFuturosPermiteDarDeBaja() throws Exception {
        long id = idDe(alta("Extracción", 45));
        Long paciente = DatosAgenda.paciente(jdbc, true);
        Long profesional = DatosAgenda.profesional(jdbc, true);
        Long sillon = DatosAgenda.sillon(jdbc, true);
        Instant inicio = Instant.now(reloj).plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.SECONDS);
        Long turnoId = DatosAgenda.turno(jdbc, paciente, profesional, sillon, id, inicio.toString(),
                inicio.plus(Duration.ofMinutes(45)).toString(), "PROGRAMADO");

        mockMvc.perform(delete(URL + "/" + id).with(csrfValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-37"));

        mockMvc.perform(post("/api/turnos/{id}/cancelar", turnoId).with(csrfValido()))
                .andExpect(status().isOk());

        mockMvc.perform(delete(URL + "/" + id).with(csrfValido()))
                .andExpect(status().isNoContent());
        assertThat(consulta.buscarPorId(id)).hasValueSatisfying(p -> assertThat(p.activa()).isFalse());
    }

    // --- Consulta del catálogo ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Una prestación inactiva no aparece con soloActivas")
    void soloActivas() throws Exception {
        alta("Consulta", 30).andExpect(status().isCreated());
        long blanqueamiento = idDe(alta("Blanqueamiento", 60));
        mockMvc.perform(delete(URL + "/" + blanqueamiento).with(csrfValido())).andExpect(status().isNoContent());

        mockMvc.perform(get(URL).param("soloActivas", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Consulta")))
                .andExpect(jsonPath("$[*].nombre").value(not(hasItem("Blanqueamiento"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Sin filtro el listado incluye las inactivas ordenadas por nombre")
    void sinFiltro() throws Exception {
        alta("Limpieza", 45).andExpect(status().isCreated());
        long blanqueamiento = idDe(alta("Blanqueamiento", 60));
        mockMvc.perform(delete(URL + "/" + blanqueamiento).with(csrfValido())).andExpect(status().isNoContent());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Blanqueamiento"))
                .andExpect(jsonPath("$[0].activa").value(false))
                .andExpect(jsonPath("$[1].nombre").value("Limpieza"));
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Consultar una prestación inexistente responde 404")
    void consultarInexistente() throws Exception {
        esperarNoEncontrada(mockMvc.perform(get(URL + "/" + INEXISTENTE)));
    }

    // --- Permisos del catálogo de prestaciones ---

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Recepción lee el catálogo")
    void recepcionLee() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("El odontólogo lee el catálogo")
    void odontologoLee() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Recepción no puede escribir prestaciones")
    void recepcionNoEscribe() throws Exception {
        esperarEscrituraProhibida();
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("El odontólogo no puede escribir prestaciones")
    void odontologoNoEscribe() throws Exception {
        esperarEscrituraProhibida();
    }

    @Test
    @DisplayName("Sin sesión el catálogo responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    // --- Ayudas ---

    private ResultActions alta(String nombre, int duracion) throws Exception {
        return mockMvc.perform(post(URL).with(csrfValido()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new PrestacionSolicitud(nombre, duracion))));
    }

    private ResultActions modificar(long id, String nombre, int duracion, boolean activa) throws Exception {
        return mockMvc.perform(put(URL + "/" + id).with(csrfValido()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new PrestacionModificacion(nombre, duracion, activa))));
    }

    private long idDe(ResultActions respuesta) throws Exception {
        String cuerpo = respuesta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(cuerpo);
        return nodo.get("id").asLong();
    }

    private static void esperarRn09(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-09"));
    }

    private static void esperarNoEncontrada(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PRESTACION-NO-ENCONTRADA"));
    }

    /** Crea una prestación como ADMIN y comprueba que el rol del test no puede crear, modificar ni dar de baja. */
    private void esperarEscrituraProhibida() throws Exception {
        String cuerpo = mockMvc.perform(post(URL).with(user("admin").roles("ADMIN")).with(csrfValido())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new PrestacionSolicitud("Limpieza", 45))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(cuerpo).get("id").asLong();

        alta("Consulta", 30)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        modificar(id, "Limpieza profunda", 60, true)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(delete(URL + "/" + id).with(csrfValido()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(consulta.buscarPorId(id)).hasValueSatisfying(prestacion -> {
            assertThat(prestacion.nombre()).isEqualTo("Limpieza");
            assertThat(prestacion.duracionDefaultMin()).isEqualTo(45);
            assertThat(prestacion.activa()).isTrue();
        });
        mockMvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(1));
    }
}
