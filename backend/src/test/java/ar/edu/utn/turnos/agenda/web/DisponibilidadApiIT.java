package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.ConsultarDisponibilidadPersonal;
import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Transactional
@Import(RelojFijoConfig.class)
class DisponibilidadApiIT extends PruebaIntegracionBase {
    static final String RUTA = "/api/agenda/disponibilidad";
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired ConsultarDisponibilidadPersonal personal;
    @Autowired DisponibilidadService disponibilidad;
    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    RequestPostProcessor recepcion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "10:00");
        recepcion = personal(Rol.RECEPCION);
    }

    RequestPostProcessor personal(Rol rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol.name());
        return TurnosApiIT.sesion((Long) usuario[0], (String) usuario[1], rol);
    }

    MockHttpServletRequestBuilder consulta() {
        return get(RUTA).param("profesionalId", profesional.toString()).param("prestacionId", prestacion.toString())
                .param("desde", "2030-06-10T09:00:00-03:00").param("hasta", "2030-06-10T10:00:00-03:00");
    }

    RequestPostProcessor parametro(String nombre, String valor) {
        return solicitud -> {
            solicitud.setParameter(nombre, valor);
            return solicitud;
        };
    }

    @Test
    @DisplayName("Recepción consulta disponibilidad y crea un turno sugerido")
    void consultaYCreacion() throws Exception {
        // Dejar un solo sillón activo para que la elección sea comprobable, con rollback al terminar.
        jdbc.update("update sillon set activo = false where id <> ?", sillon);
        var respuesta = mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7)).andExpect(jsonPath("$[0].sillonId").value(sillon))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-10T12:00:00Z"))
                .andExpect(jsonPath("$[0].fin").value("2030-06-10T12:30:00Z"))
                .andReturn().getResponse().getContentAsString();
        var sugerencia = json.readTree(respuesta).get(0);
        assertThat(sugerencia.size()).isEqualTo(3);
        var cuerpo = json.writeValueAsString(Map.of("pacienteId", paciente, "profesionalId", profesional,
                "sillonId", sugerencia.get("sillonId").asLong(), "prestacionId", prestacion,
                "inicio", sugerencia.get("inicio").asText()));
        mockMvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(recepcion).with(csrfValido())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Se puede consultar una duración ajustada")
    void duracionAjustada() throws Exception {
        mockMvc.perform(consulta().param("duracionMin", "45").with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].fin").value("2030-06-10T12:45:00Z"));
    }

    @ParameterizedTest(name = "{0} existente={1}, activo={2}")
    @CsvSource({"profesional,true,false,PROFESIONAL_INACTIVO", "prestacion,true,false,PRESTACION_INACTIVA",
            "profesional,false,true,PROFESIONAL_INEXISTENTE", "prestacion,false,true,PRESTACION_INEXISTENTE"})
    @DisplayName("Recursos inexistentes o inactivos se rechazan")
    void recursosInvalidos(String recurso, boolean existe, boolean activo, String codigo) throws Exception {
        var pedido = consulta();
        if (!existe) {
            pedido.with(parametro(recurso + "Id", "9223372036854775807"));
        } else if (recurso.equals("profesional")) {
            jdbc.update("update profesional set activo = ? where id = ?", activo, profesional);
        } else {
            jdbc.update("update prestacion set activa = ? where id = ?", activo, prestacion);
        }
        mockMvc.perform(pedido.with(recepcion)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    @DisplayName("Un profesional sin la prestación se rechaza")
    void sinPrestacion() throws Exception {
        jdbc.update("delete from profesional_prestacion where profesional_id = ?", profesional);
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-11"));
    }

    @ParameterizedTest
    @CsvSource({"2030-06-10T09:00:00-03:00", "2030-06-10T08:00:00-03:00", "2030-07-11T09:00:00.001-03:00"})
    @DisplayName("Un rango inválido se rechaza")
    void rangoInvalido(String hasta) throws Exception {
        mockMvc.perform(consulta().with(parametro("hasta", hasta)).with(recepcion)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_DISPONIBILIDAD_INVALIDO"));
    }

    @Test
    @DisplayName("Parámetros obligatorios o con formato inválido")
    void formatoInvalido() throws Exception {
        mockMvc.perform(get(RUTA).with(recepcion)).andExpect(status().isBadRequest());
        mockMvc.perform(consulta().with(parametro("desde", "2030-06-10T09:00:00")).with(recepcion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(consulta().with(parametro("profesionalId", "0")).with(recepcion))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La búsqueda respeta el día de Buenos Aires")
    void diaLocal() throws Exception {
        DatosAgenda.horario(jdbc, profesional, 1, "21:00", "23:00");
        mockMvc.perform(consulta().with(parametro("desde", "2030-06-11T00:00:00Z"))
                .with(parametro("hasta", "2030-06-11T01:00:00Z")).with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-11T00:00:00Z"));
    }

    @Test
    @DisplayName("El administrador consulta disponibilidad")
    void administrador() throws Exception {
        mockMvc.perform(consulta().with(personal(Rol.ADMIN))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("El odontólogo no consulta disponibilidad")
    void odontologo() throws Exception {
        assertThatThrownBy(() -> personal.consultar(profesional, prestacion,
                Instant.parse("2030-06-10T12:00:00Z"), Instant.parse("2030-06-10T13:00:00Z"), null))
                .isInstanceOf(AccessDeniedException.class);
        mockMvc.perform(consulta().with(personal(Rol.ODONTOLOGO))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin sesión no se consulta disponibilidad")
    void sinSesion() throws Exception {
        mockMvc.perform(consulta()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El borde contiguo de un turno queda disponible")
    void turnoOcupado() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, "2030-06-10T12:00:00Z",
                "2030-06-10T12:30:00Z", "PROGRAMADO");
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-10T12:30:00Z"));
    }

    @Test
    @DisplayName("Un bloqueo excluye solapamientos y permite el borde")
    void bloqueo() throws Exception {
        DatosAgenda.bloqueo(jdbc, profesional, "2030-06-10T12:00:00Z", "2030-06-10T12:30:00Z");
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-10T12:30:00Z"));
    }

    @Test
    @DisplayName("Los turnos cancelados no quitan disponibilidad")
    void cancelado() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, "2030-06-10T12:00:00Z",
                "2030-06-10T13:00:00Z", "CANCELADO");
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    @DisplayName("Se elige el primer sillón libre para todo el intervalo")
    void sillonDeOtroProfesionalOcupado() throws Exception {
        var otro = DatosAgenda.profesional(jdbc, true);
        var alternativo = DatosAgenda.sillon(jdbc, true);
        jdbc.update("update sillon set activo = false where id not in (?, ?)", sillon, alternativo);
        DatosAgenda.turno(jdbc, paciente, otro, sillon, prestacion, "2030-06-10T12:00:00Z",
                "2030-06-10T12:30:00Z", "PROGRAMADO");
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sillonId").value(alternativo))
                .andExpect(jsonPath("$[6].sillonId").value(sillon));
    }

    @Test
    @DisplayName("Sin sillones libres no se ofrecen horarios")
    void sinSillones() throws Exception {
        jdbc.update("update sillon set activo = false");
        mockMvc.perform(consulta().with(recepcion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("El núcleo de disponibilidad se reutiliza sin sesión del personal")
    void nucleoSinSesion() {
        assertThat(disponibilidad.consultar(profesional, prestacion,
                Instant.parse("2030-06-10T12:00:00Z"), Instant.parse("2030-06-10T13:00:00Z"), null))
                .hasSize(7).extracting(HorarioDisponible::inicio).isSorted();
    }

    @Test
    @DisplayName("Fechas fuera del calendario soportado se rechazan")
    void fechasExtremas() throws Exception {
        mockMvc.perform(consulta().with(parametro("desde", "+999999999-06-10T09:00:00Z"))
                .with(parametro("hasta", "+999999999-06-10T10:00:00Z")).with(recepcion))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_DISPONIBILIDAD_INVALIDO"));
    }
}
