package ar.edu.utn.turnos.profesionales.sillon.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.profesionales.sillon.application.ServicioSillones;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta.Datos;
import ar.edu.utn.turnos.profesionales.sillon.infrastructure.SillonesPersistencia;
import ar.edu.utn.turnos.seguridad.infrastructure.SecurityConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SillonesController.class)
@Import({SecurityConfig.class, ServicioSillones.class})
class SillonesControllerTest {

    private static final String RUTA = "/api/sillones";

    @Autowired private MockMvc mvc;
    @MockitoBean private SillonesPersistencia sillones;

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION", "ODONTOLOGO"})
    @DisplayName("Los tres roles consultan sillones activos e inactivos")
    void consultaCatalogoYDetalle(String rol) throws Exception {
        var activo = new Datos(1L, "Box norte", "Planta baja", true);
        var inactivo = new Datos(2L, "Box sur", null, false);
        when(sillones.listarTodos()).thenReturn(List.of(activo, inactivo));
        when(sillones.buscarPorId(1L)).thenReturn(Optional.of(activo));
        when(sillones.buscarPorId(2L)).thenReturn(Optional.of(inactivo));

        mvc.perform(get(RUTA).with(user("personal-ficticio").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id":1,"nombre":"Box norte","descripcion":"Planta baja","activo":true},
                          {"id":2,"nombre":"Box sur","descripcion":null,"activo":false}
                        ]
                        """));
        mvc.perform(get(RUTA + "/1").with(user("personal-ficticio").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":1,"nombre":"Box norte","descripcion":"Planta baja","activo":true}
                        """));
        mvc.perform(get(RUTA + "/2").with(user("personal-ficticio").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":2,"nombre":"Box sur","descripcion":null,"activo":false}
                        """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION", "ODONTOLOGO"})
    @DisplayName("Consultar un ID inexistente devuelve 404")
    void detalleInexistente(String rol) throws Exception {
        when(sillones.buscarPorId(99L)).thenReturn(Optional.empty());

        mvc.perform(get(RUTA + "/99").with(user("personal-ficticio").roles(rol)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("SILLON_NO_ENCONTRADO"));
    }
}
