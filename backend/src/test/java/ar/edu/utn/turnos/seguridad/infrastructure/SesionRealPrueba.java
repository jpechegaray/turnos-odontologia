package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Sesión real del personal en tests: CSRF y login como los hace la SPA (contrato de C-03). */
final class SesionRealPrueba {

    private SesionRealPrueba() {
    }

    static MockHttpSession iniciar(MockMvc mockMvc, String email, String clave) throws Exception {
        Cookie csrf = csrf(mockMvc, null);
        var resultado = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, clave)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    /** Token CSRF vigente para la sesión dada (o para una petición anónima), como cookie más cabecera. */
    static RequestPostProcessor csrfDe(MockMvc mockMvc, MockHttpSession sesion) throws Exception {
        Cookie token = csrf(mockMvc, sesion);
        return peticion -> {
            peticion.setCookies(token);
            peticion.addHeader("X-XSRF-TOKEN", token.getValue());
            return peticion;
        };
    }

    private static Cookie csrf(MockMvc mockMvc, MockHttpSession sesion) throws Exception {
        var peticion = get("/api/auth/csrf");
        if (sesion != null) {
            peticion.session(sesion);
        }
        return mockMvc.perform(peticion).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
