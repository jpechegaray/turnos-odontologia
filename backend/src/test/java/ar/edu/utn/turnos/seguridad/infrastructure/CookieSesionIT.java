package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.ContenedoresPrueba;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "APP_COOKIE_SECURE=true")
@ActiveProfiles("test")
@Import(ContenedoresPrueba.class)
class CookieSesionIT {

    @LocalServerPort private int puerto;
    @Autowired private UsuarioJpaRepository usuarios;

    @Test
    @DisplayName("La cookie de sesión real es HttpOnly, SameSite Lax y Secure")
    void emiteCookieConAtributos() throws Exception {
        String email = "cookie@consultorio.test";
        String clave = "clave-fixture-cookie";
        usuarios.saveAndFlush(new Usuario(email, new BCryptPasswordEncoder().encode(clave),
                "Personal Ficticio", Rol.ADMIN, Instant.parse("2026-11-21T12:00:00Z")));

        try (var cliente = HttpClient.newHttpClient()) {
            String cookie = ingresar(cliente, email, clave);
            assertThat(cookie.toLowerCase(Locale.ROOT)).contains("; httponly", "; samesite=lax", "; secure");
            var me = consultarSesion(cliente, cookie.split(";")[0]);
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(me.body()).contains(email);
        }
    }

    @Test
    @DisplayName("Cerrar sesión invalida el acceso posterior")
    void cookieAnteriorNoAutorizaTrasElLogout() throws Exception {
        String email = "logout-http@consultorio.test";
        String clave = "clave-fixture-logout";
        usuarios.saveAndFlush(new Usuario(email, new BCryptPasswordEncoder().encode(clave),
                "Personal Ficticio", Rol.ADMIN, Instant.parse("2026-11-21T12:00:00Z")));

        try (var cliente = HttpClient.newHttpClient()) {
            String sesion = ingresar(cliente, email, clave).split(";")[0];
            assertThat(consultarSesion(cliente, sesion).statusCode()).isEqualTo(200);
            String csrf = obtenerCsrf(cliente, sesion);
            var logout = cliente.send(HttpRequest.newBuilder(url("/api/auth/logout"))
                            .header("Cookie", sesion + "; " + csrf)
                            .header("X-XSRF-TOKEN", csrf.substring("XSRF-TOKEN=".length()))
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(logout.statusCode()).isEqualTo(204);

            var posterior = consultarSesion(cliente, sesion);
            assertThat(posterior.statusCode()).isEqualTo(401);
            assertThat(posterior.headers().firstValue("Content-Type").orElseThrow())
                    .startsWith("application/problem+json");
        }
    }

    private String ingresar(HttpClient cliente, String email, String clave) throws Exception {
        String csrf = obtenerCsrf(cliente, null);
        var respuesta = cliente.send(HttpRequest.newBuilder(url("/api/auth/login"))
                        .header("Cookie", csrf)
                        .header("X-XSRF-TOKEN", csrf.substring("XSRF-TOKEN=".length()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, clave))).build(), HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return cookie(respuesta, "JSESSIONID");
    }

    private String obtenerCsrf(HttpClient cliente, String sesion) throws Exception {
        var peticion = HttpRequest.newBuilder(url("/api/auth/csrf"));
        if (sesion != null) {
            peticion.header("Cookie", sesion);
        }
        var respuesta = cliente.send(peticion.GET().build(), HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(204);
        return cookie(respuesta, "XSRF-TOKEN").split(";")[0];
    }

    private HttpResponse<String> consultarSesion(HttpClient cliente, String sesion) throws Exception {
        return cliente.send(HttpRequest.newBuilder(url("/api/auth/me")).header("Cookie", sesion).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String cookie(HttpResponse<?> respuesta, String nombre) {
        return respuesta.headers().allValues("Set-Cookie").stream()
                .filter(valor -> valor.startsWith(nombre + "=")).findFirst().orElseThrow();
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
