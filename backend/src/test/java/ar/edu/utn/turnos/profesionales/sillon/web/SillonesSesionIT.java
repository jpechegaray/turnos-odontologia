package ar.edu.utn.turnos.profesionales.sillon.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SillonesSesionIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/sillones";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CLAVE_FIXTURE = "clave-fixture-sillones";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationContext contexto;

    @BeforeEach
    void requiereContratoC03() {
        assumeTrue(contexto.getBeanProvider(CookieCsrfTokenRepository.class).getIfAvailable() != null,
                "La comprobación de sesión y CSRF por cookie requiere C-03");
    }

    @Test
    @DisplayName("Una sesión real de administrador puede crear y editar con cookie y cabecera CSRF")
    void escribeConSesionReal() throws Exception {
        var sesion = ingresar("alta");
        var csrf = obtenerCsrf(sesion);

        String ubicacion = mockMvc.perform(conCsrf(post(RUTA), csrf).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Box con sesión"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getHeader("Location");
        assertThat(ubicacion).startsWith(RUTA + "/");
        mockMvc.perform(conCsrf(put(ubicacion), csrf).session(sesion)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Box con sesión","activo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("Una sesión real de administrador no escribe con CSRF ausente o inválido")
    void rechazaCsrfDeSesionReal() throws Exception {
        var sesion = ingresar("rechazo");
        var csrf = obtenerCsrf(sesion);
        String nombre = "Box no creado por sesión";

        mockMvc.perform(alta(nombre).session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(alta(nombre).session(sesion).cookie(csrf).header(CSRF_HEADER, "invalido"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("select count(*) from sillon where nombre = ?", Long.class, nombre)).isZero();
    }

    @Test
    @DisplayName("Una escritura anónima sin cookie o cabecera CSRF recibe 401")
    void rechazaAnonimoSinCsrfCon401() throws Exception {
        String nombre = "Box no creado por anónimo";

        mockMvc.perform(alta(nombre))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put(RUTA + "/{id}", Long.MAX_VALUE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Box no creado por anónimo","activo":true}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForObject("select count(*) from sillon where nombre = ?", Long.class, nombre)).isZero();
    }

    private MockHttpSession ingresar(String caso) throws Exception {
        String email = "sillones-" + caso + "@consultorio.test";
        jdbc.update("""
                insert into usuario (email, password_hash, nombre, rol, activo, intentos_fallidos, creado_en)
                values (?, ?, ?, 'ADMIN', true, 0, now())
                """, email, new BCryptPasswordEncoder().encode(CLAVE_FIXTURE), "Administrador ficticio");
        var respuesta = mockMvc.perform(conCsrf(post("/api/auth/login"), obtenerCsrf(null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, CLAVE_FIXTURE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("ADMIN")).andReturn();
        var sesion = (MockHttpSession) respuesta.getRequest().getSession(false);
        assertThat(sesion).isNotNull();
        return sesion;
    }

    private Cookie obtenerCsrf(MockHttpSession sesion) throws Exception {
        var peticion = get("/api/auth/csrf");
        if (sesion != null) {
            peticion.session(sesion);
        }
        var cookie = mockMvc.perform(peticion).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie(CSRF_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private MockHttpServletRequestBuilder alta(String nombre) {
        return post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"%s"}
                """.formatted(nombre));
    }

    private MockHttpServletRequestBuilder conCsrf(MockHttpServletRequestBuilder peticion, Cookie cookie) {
        return peticion.cookie(cookie).header(CSRF_HEADER, cookie.getValue());
    }
}
