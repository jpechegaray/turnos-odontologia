package ar.edu.utn.turnos.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({ManejadorErrores.class, ManejadorErroresTest.ControladorPrueba.class})
@WithMockUser
class ManejadorErroresTest {

    record SolicitudPrueba(@NotBlank String nombre) {
    }

    @RestController
    static class ControladorPrueba {

        @GetMapping("/prueba/regla")
        void regla() {
            throw new ReglaNegocioException("RN-04", EstadoError.NO_PROCESABLE_422,
                    "No se puede reservar un turno en el pasado");
        }

        @GetMapping("/prueba/regla/{estado}")
        void reglaConEstado(@PathVariable EstadoError estado) {
            throw new ReglaNegocioException("RN-99", estado, "Regla de prueba");
        }

        @GetMapping("/prueba/conflicto")
        void conflicto() {
            throw new ReglaNegocioException("RN-01", EstadoError.CONFLICTO_409,
                    "El profesional ya tiene un turno en ese horario", Map.of("recurso", "PROFESIONAL"));
        }

        @PostMapping("/prueba/validacion")
        void validacion(@Valid @RequestBody SolicitudPrueba solicitud) {
        }

        @GetMapping("/prueba/inesperado")
        void inesperado() {
            throw new RuntimeException("SELECT secreto FROM pacientes WHERE dni = '00000000'");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Un error de regla responde Problem Details con su código")
    void errorDeReglaRespondeProblemDetailsConSuCodigo() throws Exception {
        mockMvc.perform(get("/prueba/regla"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").value("No se puede reservar un turno en el pasado"))
                .andExpect(jsonPath("$.codigo").value("RN-04"));
    }

    @ParameterizedTest(name = "estado {0}")
    @CsvSource({"PROHIBIDO_403,403", "NO_ENCONTRADO_404,404", "CONFLICTO_409,409", "NO_PROCESABLE_422,422"})
    @DisplayName("Cada estado de regla responde con el mismo formato")
    void cadaEstadoDeReglaRespondeConElMismoFormato(EstadoError estado, int esperado) throws Exception {
        mockMvc.perform(get("/prueba/regla/" + estado.name()))
                .andExpect(status().is(esperado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(esperado))
                .andExpect(jsonPath("$.detail").value("Regla de prueba"))
                .andExpect(jsonPath("$.codigo").value("RN-99"));
    }

    @Test
    @DisplayName("Una regla puede agregar datos de contexto al error")
    void reglaPuedeAgregarDatosDeContextoAlError() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"))
                .andExpect(jsonPath("$.recurso").value("PROFESIONAL"));
    }

    @Test
    @DisplayName("Una petición con campos inválidos responde 400 Problem Details")
    void peticionConCamposInvalidosResponde400ProblemDetails() throws Exception {
        mockMvc.perform(post("/prueba/validacion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.length()").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[0].valorRechazado").doesNotExist());
    }

    @Test
    @DisplayName("Una petición con un campo ausente también lista el campo inválido")
    void peticionConCampoAusenteListaElCampoInvalido() throws Exception {
        mockMvc.perform(post("/prueba/validacion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"));
    }

    @Test
    @DisplayName("Un error inesperado no expone detalles internos")
    void errorInesperadoNoExponeDetallesInternos() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("SELECT", "secreto", "pacientes", "RuntimeException", "at ar.edu");
    }
}
