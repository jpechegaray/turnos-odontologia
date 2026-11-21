package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.shared.web.ManejadorErrores;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, ManejadorErrores.class, SecurityConfigTest.ControladorPrueba.class})
class SecurityConfigTest {

    @RestController
    static class ControladorPrueba {

        @GetMapping("/api/public/prueba")
        public String publicaGet() {
            return "ok";
        }

        @PostMapping("/api/public/prueba")
        public String publicaPost() {
            return "ok";
        }

        @GetMapping("/api/prueba/recepcion")
        @PreAuthorize("hasRole('RECEPCION')")
        public String soloRecepcion() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Una petición anónima a una ruta protegida recibe 401")
    void peticionAnonimaARutaProtegidaRecibe401() throws Exception {
        mockMvc.perform(get("/api/turnos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Una ruta protegida inexistente también recibe 401")
    void rutaProtegidaInexistenteTambienRecibe401() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una ruta protegida existente anónima recibe 401 y no 200")
    void rutaProtegidaExistenteAnonimaRecibe401() throws Exception {
        mockMvc.perform(get("/api/prueba/recepcion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una ruta pública responde sin autenticación")
    void rutaPublicaRespondeSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/public/prueba"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una escritura pública no exige token CSRF")
    void escrituraPublicaNoExigeTokenCsrf() throws Exception {
        mockMvc.perform(post("/api/public/prueba"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una escritura protegida sin token CSRF es rechazada")
    void escrituraProtegidaSinCsrfEsRechazada() throws Exception {
        mockMvc.perform(post("/api/turnos"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("Un usuario sin el rol requerido recibe 403 Problem Details")
    void usuarioSinElRolRequeridoRecibe403ProblemDetails() throws Exception {
        mockMvc.perform(get("/api/prueba/recepcion"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Un usuario con el rol requerido accede a la operación")
    void usuarioConElRolRequeridoAccedeALaOperacion() throws Exception {
        mockMvc.perform(get("/api/prueba/recepcion"))
                .andExpect(status().isOk());
    }
}
