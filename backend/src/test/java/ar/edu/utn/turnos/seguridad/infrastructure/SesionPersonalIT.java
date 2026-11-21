package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(OutputCaptureExtension.class)
@Import(SesionPersonalIT.ConfiguracionPrueba.class)
class SesionPersonalIT extends PruebaIntegracionBase {

    static final String CLAVE = "clave-fixture-sesion";
    private static final String ESCRITURA = "/api/prueba/csrf-c03";
    private static final String EJECUTADA = "escrituraC03Ejecutada";
    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Un usuario activo inicia sesión y consulta su identidad")
    void ingresaYConsulta() throws Exception {
        String email = "sesion@consultorio.test";
        guardar(email);
        var resultado = mockMvc.perform(login("Sesion@consultorio.test", CLAVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("ODONTOLOGO"))
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        mockMvc.perform(get("/api/auth/me").session((MockHttpSession) resultado.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().json(resultado.getResponse().getContentAsString()));
    }

    @Test
    @DisplayName("El inicio de sesión no reutiliza una sesión anterior")
    void rotaIdentificador() throws Exception {
        String email = "rotacion@consultorio.test";
        guardar(email);
        var sesion = new MockHttpSession();
        String idAnterior = sesion.getId();

        var resultado = mockMvc.perform(login(email, CLAVE).session(sesion))
                .andExpect(status().isOk()).andReturn();

        assertThat(resultado.getRequest().getSession(false).getId()).isNotEqualTo(idAnterior);
    }

    @Test
    @DisplayName("El rechazo de login no revela el estado de la cuenta")
    void rechazaSinDistinguirCausa() throws Exception {
        String activa = "activa@consultorio.test";
        String inactiva = "baja@consultorio.test";
        String bloqueada = "bloqueada@consultorio.test";
        guardar(activa);
        guardar(inactiva);
        guardar(bloqueada);
        jdbc.update("update usuario set activo = false where email = ?", inactiva);
        jdbc.update("update usuario set bloqueado_hasta = now() + interval '1 hour' where email = ?", bloqueada);
        String esperado = rechazo("inexistente@consultorio.test", CLAVE);

        assertThat(rechazo(activa, "incorrecta")).isEqualTo(esperado);
        assertThat(rechazo(inactiva, CLAVE)).isEqualTo(esperado);
        assertThat(rechazo(bloqueada, CLAVE)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Consultar la sesión sin autenticar devuelve 401")
    void requiereSesion() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Cerrar sesión invalida el objeto de sesión en MockMvc")
    void cierraSesion() throws Exception {
        String email = "salida@consultorio.test";
        guardar(email);
        MockHttpSession sesion = ingresar(email);
        Cookie csrf = obtenerCsrf(sesion);
        mockMvc.perform(salida(sesion, csrf)).andExpect(status().isNoContent());
        assertThat(sesion.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("JSESSIONID", sesion.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La SPA obtiene CSRF sin sesión y lo renueva al cambiar de sesión")
    void renuevaCsrf() throws Exception {
        String email = "renovacion@consultorio.test";
        guardar(email);
        Cookie inicial = obtenerCsrf(null);
        var resultado = mockMvc.perform(login(email, CLAVE, inicial)).andExpect(status().isOk()).andReturn();
        assertThat(resultado.getResponse().getCookie("XSRF-TOKEN").getMaxAge()).isZero();
        var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        Cookie autenticada = obtenerCsrf(sesion);
        assertThat(autenticada.getValue()).isNotEqualTo(inicial.getValue());

        var salida = mockMvc.perform(salida(sesion, autenticada)).andExpect(status().isNoContent()).andReturn();
        assertThat(salida.getResponse().getCookie("XSRF-TOKEN").getMaxAge()).isZero();
        Cookie nueva = obtenerCsrf(null);
        assertThat(nueva.getValue()).isNotEqualTo(autenticada.getValue());
        mockMvc.perform(login(email, CLAVE, nueva)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una operación que requiere CSRF rechaza tokens faltantes o inválidos")
    void rechazaCsrfFaltanteOInvalido() throws Exception {
        String email = "csrf@consultorio.test";
        guardar(email);
        MockHttpSession sesion = ingresar(email);
        Cookie csrf = obtenerCsrf(sesion);
        var loginSinCsrf = post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, CLAVE));
        assertThat(rechazoCsrf(loginSinCsrf).getRequest().getSession(false)).isNull();
        rechazoCsrf(post("/api/auth/logout").session(sesion));
        rechazoCsrf(post("/api/auth/logout").session(sesion).cookie(csrf).header("X-XSRF-TOKEN", "invalido"));
        assertThat(sesion.isInvalid()).isFalse();
        mockMvc.perform(get("/api/auth/me").session(sesion)).andExpect(status().isOk());

        rechazoCsrf(post(ESCRITURA).session(sesion));
        rechazoCsrf(post(ESCRITURA).session(sesion).cookie(csrf).header("X-XSRF-TOKEN", "invalido"));
        assertThat(sesion.getAttribute(EJECUTADA)).isNull();

        mockMvc.perform(post(ESCRITURA).session(sesion).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk());
        assertThat(sesion.getAttribute(EJECUTADA)).isEqualTo(true);
    }

    private org.springframework.test.web.servlet.MvcResult rechazoCsrf(MockHttpServletRequestBuilder peticion)
            throws Exception {
        return mockMvc.perform(peticion).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403)).andReturn();
    }

    @Test
    @DisplayName("Los registros no contienen credenciales ni cookies")
    void noRegistraSecretos(CapturedOutput logs) throws Exception {
        String email = "logs@consultorio.test";
        String hash = guardar(email);
        Cookie csrfAnonimo = obtenerCsrf(null);
        mockMvc.perform(login(email, "incorrecta-no-log", csrfAnonimo)).andExpect(status().isUnauthorized());
        MockHttpSession sesion = ingresar(email);
        Cookie csrf = obtenerCsrf(sesion);
        mockMvc.perform(salida(sesion, csrf)).andExpect(status().isNoContent());

        assertThat(logs.getAll()).doesNotContain(CLAVE, hash, "incorrecta-no-log",
                csrfAnonimo.getValue(), csrf.getValue(), "JSESSIONID=");
    }

    private MockHttpSession ingresar(String email) throws Exception {
        var resultado = mockMvc.perform(login(email, CLAVE)).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private Cookie obtenerCsrf(MockHttpSession sesion) throws Exception {
        var peticion = get("/api/auth/csrf");
        if (sesion != null) {
            peticion.session(sesion);
        }
        return mockMvc.perform(peticion).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private MockHttpServletRequestBuilder salida(MockHttpSession sesion, Cookie csrf) {
        return post("/api/auth/logout").session(sesion).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
    }

    private MockHttpServletRequestBuilder login(String email, String clave) throws Exception {
        return login(email, clave, obtenerCsrf(null));
    }

    private MockHttpServletRequestBuilder login(String email, String clave, Cookie csrf) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, clave));
    }

    private String rechazo(String email, String clave) throws Exception {
        var resultado = mockMvc.perform(login(email, clave))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();
        assertThat(resultado.getRequest().getSession(false)).isNull();
        return resultado.getResponse().getContentAsString();
    }

    private String guardar(String email) {
        String hash = new BCryptPasswordEncoder().encode(CLAVE);
        usuarios.saveAndFlush(new Usuario(email, hash, "Personal Ficticio",
                Rol.ODONTOLOGO, Instant.parse("2026-11-21T12:00:00Z")));
        return hash;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracionPrueba {
        @Bean
        ControladorPruebaCsrf controladorPruebaCsrf() {
            return new ControladorPruebaCsrf();
        }
    }

    @RestController
    static class ControladorPruebaCsrf {
        @PostMapping(ESCRITURA)
        void escribir(HttpSession sesion) {
            sesion.setAttribute(EJECUTADA, true);
        }
    }
}
