package ar.edu.utn.turnos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base de las pruebas de integración: contexto completo, perfil {@code test} y PostgreSQL real.
 *
 * <p>Para no arrancar un contenedor nuevo por cada clase, las subclases deben preferir
 * {@code @Import} de configuraciones compartidas antes que {@code @MockitoBean} o propiedades
 * propias: cualquier cambio en la configuración del contexto crea un contexto (y un contenedor) nuevo.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
@AutoConfigureMockMvc
public abstract class PruebaIntegracionBase {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Token CSRF real, como lo obtiene la SPA: cookie {@code XSRF-TOKEN} de {@code GET /api/auth/csrf}
     * enviada además en la cabecera {@code X-XSRF-TOKEN}. No se usa el post-procesador de CSRF de
     * spring-security-test: reemplaza el repositorio de tokens del contexto compartido y rompe a las demás clases.
     */
    protected RequestPostProcessor csrfValido() throws Exception {
        Cookie token = obtenerCookieCsrf();
        return peticion -> {
            peticion.setCookies(token);
            peticion.addHeader("X-XSRF-TOKEN", token.getValue());
            return peticion;
        };
    }

    /** Cookie {@code XSRF-TOKEN} real con una cabecera {@code X-XSRF-TOKEN} que no coincide: debe dar 403. */
    protected RequestPostProcessor csrfConCabeceraIncorrecta() throws Exception {
        Cookie token = obtenerCookieCsrf();
        return peticion -> {
            peticion.setCookies(token);
            peticion.addHeader("X-XSRF-TOKEN", "incorrecto");
            return peticion;
        };
    }

    private Cookie obtenerCookieCsrf() throws Exception {
        Cookie token = mockMvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        if (token == null) {
            throw new IllegalStateException("GET /api/auth/csrf no devolvió la cookie XSRF-TOKEN");
        }
        return token;
    }
}
