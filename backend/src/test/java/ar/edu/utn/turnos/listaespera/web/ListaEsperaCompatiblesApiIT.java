package ar.edu.utn.turnos.listaespera.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.application.ServicioListaEspera;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /api/lista-espera/compatibles} de punta a punta (RN-22, US-018). El reloj fijo hace que ahora sea
 * 2026-12-01 09:00 en Buenos Aires; el hueco de referencia es el jueves 3 de 10:00 a 10:30.
 */
@Transactional
@Import(RelojFijoConfig.class)
class ListaEsperaCompatiblesApiIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/lista-espera/compatibles";
    private static final String PERSONAL = "personal-ficticio";
    private static final String INICIO = "2026-12-03T10:00:00-03:00";
    private static final String FIN = "2026-12-03T10:30:00-03:00";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioListaEspera servicio;

    private Long profesional;
    private Long consulta;

    @BeforeEach
    void preparar() {
        profesional = DatosAgenda.profesional(jdbc, true);
        consulta = prestacionDe(30);
    }

    private Long prestacionDe(int minutos) {
        var prestacion = DatosAgenda.prestacion(jdbc, minutos, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        return prestacion;
    }

    private Long paciente() {
        return DatosFicticios.paciente(jdbc, "compatibles@correo.test");
    }

    private Long entrada(Long paciente, Long prestacion, Long preferido, String estado, String creadoEn) {
        return DatosFicticios.entrada(jdbc, paciente, prestacion, preferido, "2026-12-01", "2026-12-31", null, null,
                estado, creadoEn);
    }

    private MockHttpServletRequestBuilder consultar(Object prof, String inicio, String fin) {
        var peticion = get(RUTA);
        if (prof != null) {
            peticion.param("profesionalId", prof.toString());
        }
        if (inicio != null) {
            peticion.param("inicio", inicio);
        }
        if (fin != null) {
            peticion.param("fin", fin);
        }
        return peticion;
    }

    private List<Integer> ids(MockHttpServletRequestBuilder peticion, String rol) throws Exception {
        var cuerpo = mockMvc.perform(peticion.with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$[*].id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Recepción ve solo las entradas compatibles")
    void soloCompatibles(String rol) throws Exception {
        var otroProfesional = DatosAgenda.profesional(jdbc, true);
        var compatible = entrada(paciente(), consulta, null, "ACTIVA", "2026-11-01T10:00:00Z");
        entrada(paciente(), prestacionDe(90), null, "ACTIVA", "2026-11-01T10:00:00Z");
        entrada(paciente(), consulta, otroProfesional, "ACTIVA", "2026-11-01T10:00:00Z");
        entrada(paciente(), consulta, null, "CANCELADA", "2026-11-01T10:00:00Z");

        mockMvc.perform(consultar(profesional, INICIO, FIN).with(user(PERSONAL).roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(compatible))
                .andExpect(jsonPath("$[0].prestacionId").value(consulta))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    @DisplayName("Las sugerencias se ordenan por antigüedad y muestran todas las entradas del paciente")
    void ordenYTodasLasDelPaciente() throws Exception {
        var a = paciente();
        var nuevaDeA = entrada(a, prestacionDe(20), null, "ACTIVA", "2026-11-05T10:00:00Z");
        var deB = entrada(paciente(), consulta, null, "ACTIVA", "2026-11-03T10:00:00Z");
        var viejaDeA = entrada(a, consulta, null, "ACTIVA", "2026-11-01T10:00:00Z");

        assertThat(ids(consultar(profesional, INICIO, FIN), "ADMIN"))
                .containsExactly(viejaDeA.intValue(), deB.intValue(), nuevaDeA.intValue());
    }

    @Test
    @DisplayName("Un hueco que ya empezó no tiene sugerencias")
    void huecoEmpezado() throws Exception {
        entrada(paciente(), consulta, null, "ACTIVA", "2026-11-01T10:00:00Z");
        var ahora = RelojFijoConfig.INSTANTE_FIJO;

        assertThat(ids(consultar(profesional, ahora.toString(), ahora.plusSeconds(1800).toString()), "RECEPCION"))
                .isEmpty();
        // Un instante después del inicio tampoco.
        assertThat(ids(consultar(profesional, ahora.minusSeconds(60).toString(),
                ahora.plusSeconds(1800).toString()), "RECEPCION")).isEmpty();
    }

    @Test
    @DisplayName("La consulta rechaza un hueco inválido")
    void huecoInvalido() throws Exception {
        for (var peticion : List.of(
                consultar(null, INICIO, FIN),
                consultar(profesional, null, FIN),
                consultar(profesional, INICIO, null),
                consultar(profesional, "2026-12-03 10:00", FIN),
                consultar("no-numerico", INICIO, FIN),
                consultar(profesional, INICIO, INICIO),
                consultar(profesional, FIN, INICIO))) {
            mockMvc.perform(peticion.with(user(PERSONAL).roles("RECEPCION")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    @DisplayName("La consulta rechaza un profesional inexistente o inactivo")
    void profesionalInvalido() throws Exception {
        var inactivo = DatosAgenda.profesional(jdbc, false);
        for (var caso : Map.of(-1L, "PROFESIONAL_INEXISTENTE", inactivo, "PROFESIONAL_INACTIVO").entrySet()) {
            mockMvc.perform(consultar(caso.getKey(), INICIO, FIN).with(user(PERSONAL).roles("RECEPCION")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.codigo").value(caso.getValue()));
        }
    }

    @Test
    @DisplayName("El odontólogo no consulta las entradas compatibles")
    void odontologo() throws Exception {
        mockMvc.perform(consultar(profesional, INICIO, FIN).with(user(PERSONAL).roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        // Ni siquiera con parámetros inválidos llega a validar.
        mockMvc.perform(consultar(null, null, null).with(user(PERSONAL).roles("ODONTOLOGO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin sesión no se consultan las entradas compatibles")
    void sinSesion() throws Exception {
        mockMvc.perform(consultar(profesional, INICIO, FIN))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("El servicio de compatibles rechaza al odontólogo aunque se saltee el filtro HTTP")
    void servicioRechazaAlOdontologo() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "odontologo-ficticio", null, List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO"))));
        try {
            assertThatThrownBy(() -> servicio.compatibles(profesional, Instant.parse("2026-12-03T13:00:00Z"),
                    Instant.parse("2026-12-03T13:30:00Z")))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
