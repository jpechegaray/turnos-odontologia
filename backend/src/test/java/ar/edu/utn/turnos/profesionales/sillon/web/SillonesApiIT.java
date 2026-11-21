package ar.edu.utn.turnos.profesionales.sillon.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

class SillonesApiIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/sillones";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private TransactionTemplate transacciones;
    @Autowired private SillonConsulta consulta;
    @Autowired(required = false) private CookieCsrfTokenRepository csrfDeSesion;

    @Test
    @DisplayName("El administrador da de alta un sillón activo")
    void daDeAlta() throws Exception {
        var respuesta = mockMvc.perform(comoAdmin(post(RUTA), """
                        {"nombre":"  Box alta  "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Box alta"))
                .andExpect(jsonPath("$.descripcion").value(nullValue()))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse();
        Long id = jdbc.queryForObject("select id from sillon where nombre = ?", Long.class, "Box alta");
        var idRespuesta = json.readTree(respuesta.getContentAsByteArray()).path("id");
        assertThat(idRespuesta.isIntegralNumber()).isTrue();
        assertThat(idRespuesta.bigIntegerValue()).isEqualTo(BigInteger.valueOf(id));
        assertThat(respuesta.getHeader("Location")).isEqualTo(RUTA + "/" + id);
    }

    @Test
    @DisplayName("El administrador edita un sillón sin cambiar su identidad")
    void edita() throws Exception {
        Long id = guardar("Box antes de edición", true);

        mockMvc.perform(comoAdmin(put(RUTA + "/{id}", id), """
                        {"nombre":"  Box editado  ","descripcion":"Sector norte","activo":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Box editado"))
                .andExpect(jsonPath("$.descripcion").value("Sector norte"))
                .andExpect(jsonPath("$.activo").value(true));
        assertThat(datos(id)).containsEntry("nombre", "Box editado").containsEntry("descripcion", "Sector norte");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("El nombre duplicado devuelve 409 incluso si el otro sillón está inactivo")
    void impideDuplicados(boolean activo) throws Exception {
        String nombre = "Box ocupado " + activo;
        Long ocupado = guardar(nombre, activo);
        Long otro = guardar("Box libre " + activo, true);
        var originalOcupado = datos(ocupado);
        var originalOtro = datos(otro);

        mockMvc.perform(comoAdmin(post(RUTA), """
                        {"nombre":"  %s  "}
                        """.formatted(nombre)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("SILLON_NOMBRE_DUPLICADO"));
        mockMvc.perform(comoAdmin(put(RUTA + "/{id}", otro), """
                        {"nombre":"  %s  ","activo":true}
                        """.formatted(nombre)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("SILLON_NOMBRE_DUPLICADO"));

        assertThat(cantidadConNombre(nombre)).isOne();
        assertThat(datos(ocupado)).isEqualTo(originalOcupado);
        assertThat(datos(otro)).isEqualTo(originalOtro);
    }

    @Test
    @DisplayName("Dos altas concurrentes del mismo nombre no crean duplicados")
    void soloUnaAltaConcurrentePersiste() throws Exception {
        String nombre = "Box concurrente";
        var listos = new CyclicBarrier(2);
        Callable<MockHttpServletResponse> alta = () -> {
            listos.await(10, TimeUnit.SECONDS);
            return mockMvc.perform(comoAdmin(post(RUTA), """
                    {"nombre":"%s"}
                    """.formatted(nombre))).andReturn().getResponse();
        };

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primera = ejecutor.submit(alta);
            var segunda = ejecutor.submit(alta);
            var respuestas = List.of(primera.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));

            assertThat(respuestas).extracting(MockHttpServletResponse::getStatus)
                    .containsExactlyInAnyOrder(201, 409);
            var rechazo = respuestas.stream().filter(respuesta -> respuesta.getStatus() == 409).findFirst().orElseThrow();
            assertThat(rechazo.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            assertThat(rechazo.getContentAsString()).contains("SILLON_NOMBRE_DUPLICADO");
        }

        Long cantidad = transacciones.execute(estado -> cantidadConNombre(nombre));
        assertThat(cantidad).isOne();
    }

    @ParameterizedTest
    @MethodSource("escriturasInvalidas")
    @DisplayName("Una escritura con campos inválidos devuelve 400")
    void validaCampos(String caso, boolean actualizar, String cuerpo, String campo) throws Exception {
        Long id = guardar("Box validación " + caso, true);
        var original = datos(id);
        Long cantidad = jdbc.queryForObject("select count(*) from sillon", Long.class);

        mockMvc.perform(comoAdmin(actualizar ? put(RUTA + "/{id}", id) : post(RUTA), cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores[0].campo").value(campo));
        assertThat(jdbc.queryForObject("select count(*) from sillon", Long.class)).isEqualTo(cantidad);
        assertThat(datos(id)).isEqualTo(original);
    }

    static Stream<Arguments> escriturasInvalidas() {
        return Stream.of(
                Arguments.of("alta sin nombre", false, "{\"nombre\":\"   \"}", "nombre"),
                Arguments.of("edición sin nombre", true, "{\"nombre\":\"\",\"activo\":false}", "nombre"),
                Arguments.of("estado omitido", true, "{\"nombre\":\"Box válido\"}", "activo"),
                Arguments.of("estado textual", true, "{\"nombre\":\"Box válido\",\"activo\":\"false\"}", "activo"));
    }

    @Test
    @DisplayName("Actualizar un ID inexistente devuelve 404")
    void noCreaAlEditarAusente() throws Exception {
        mockMvc.perform(comoAdmin(put(RUTA + "/{id}", Long.MAX_VALUE), """
                        {"nombre":"Box ausente","activo":true}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("SILLON_NO_ENCONTRADO"));
        assertThat(cantidadConNombre("Box ausente")).isZero();
    }

    @Test
    @DisplayName("Desactivar un sillón conserva su registro")
    void desactiva() throws Exception {
        verificarTransicion("Box de baja", true, false);
    }

    @Test
    @DisplayName("Reactivar un sillón conserva su registro")
    void reactiva() throws Exception {
        verificarTransicion("Box de reingreso", false, true);
    }

    private void verificarTransicion(String nombre, boolean antes, boolean despues) throws Exception {
        Long id = guardar(nombre, antes);
        assertThat(apareceEnAgenda(id)).isEqualTo(antes);

        mockMvc.perform(comoAdmin(put(RUTA + "/{id}", id), """
                        {"nombre":"%s","activo":%s}
                        """.formatted(nombre, despues)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.activo").value(despues));
        mockMvc.perform(get(RUTA + "/{id}", id).with(user("admin-ficticio").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.activo").value(despues));
        mockMvc.perform(get(RUTA).with(user("admin-ficticio").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].activo".formatted(id), contains(despues)));
        assertThat(consulta.buscarPorId(id)).contains(new SillonConsulta.Datos(id, nombre, null, despues));
        assertThat(apareceEnAgenda(id)).isEqualTo(despues);
        assertThat(cantidadConNombre(nombre)).isOne();
    }

    private boolean apareceEnAgenda(Long id) {
        return consulta.listarActivos().stream().anyMatch(sillon -> sillon.id().equals(id));
    }

    @ParameterizedTest
    @MethodSource("rolesYEscrituras")
    @DisplayName("Recepción y odontología no pueden escribir en el catálogo")
    void rolesDeLecturaNoEscriben(String rol, Escritura operacion) throws Exception {
        Long id = guardar("Box permiso " + rol + operacion, operacion.activoInicial);
        var antes = datos(id);
        String nombreNuevo = "Box rechazado por rol";
        var solicitud = operacion.solicitud(id, nombreNuevo).with(user("personal-ficticio").roles(rol));

        problema(mockMvc.perform(conCsrfValido(solicitud)), 403);

        assertThat(cantidadConNombre(nombreNuevo)).isZero();
        assertThat(datos(id)).isEqualTo(antes);
    }

    static Stream<Arguments> rolesYEscrituras() {
        return Stream.of("RECEPCION", "ODONTOLOGO").flatMap(rol ->
                Stream.of(Escritura.values()).map(operacion -> Arguments.of(rol, operacion)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LISTADO", "DETALLE", "ALTA", "EDICION", "BAJA", "REACTIVACION"})
    @DisplayName("Un cliente sin sesión no puede acceder al catálogo")
    void requiereSesion(String acceso) throws Exception {
        var operacion = switch (acceso) {
            case "LISTADO", "DETALLE" -> null;
            default -> Escritura.valueOf(acceso);
        };
        Long id = guardar("Box anónimo " + acceso, operacion == null || operacion.activoInicial);
        var antes = datos(id);
        String nombreNuevo = "Box rechazado sin sesión";
        var solicitud = switch (acceso) {
            case "LISTADO" -> get(RUTA);
            case "DETALLE" -> get(RUTA + "/{id}", id);
            default -> conCsrfValido(operacion.solicitud(id, nombreNuevo));
        };

        problema(mockMvc.perform(solicitud), 401);

        assertThat(cantidadConNombre(nombreNuevo)).isZero();
        assertThat(datos(id)).isEqualTo(antes);
    }

    @ParameterizedTest
    @MethodSource("escriturasYCsrf")
    @DisplayName("Un administrador sin CSRF válido no puede escribir en el catálogo")
    void adminRequiereCsrf(Escritura operacion, boolean invalido) throws Exception {
        Long id = guardar("Box CSRF " + operacion + " " + invalido, operacion.activoInicial);
        var antes = datos(id);
        String nombreNuevo = "Box rechazado por CSRF";
        var solicitud = operacion.solicitud(id, nombreNuevo).with(user("admin-ficticio").roles("ADMIN"));
        if (invalido) {
            if (csrfDeSesion == null) {
                solicitud.with(csrf().asHeader().useInvalidToken());
            } else {
                solicitud.cookie(new Cookie("XSRF-TOKEN", "valor-ficticio")).header("X-XSRF-TOKEN", "incorrecto");
            }
        }

        problema(mockMvc.perform(solicitud), 403);

        assertThat(cantidadConNombre(nombreNuevo)).isZero();
        assertThat(datos(id)).isEqualTo(antes);
    }

    static Stream<Arguments> escriturasYCsrf() {
        return Stream.of(Escritura.values()).flatMap(operacion ->
                Stream.of(false, true).map(invalido -> Arguments.of(operacion, invalido)));
    }

    private void problema(ResultActions respuesta, int codigo) throws Exception {
        respuesta.andExpect(status().is(codigo))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(codigo))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private enum Escritura {
        ALTA(true, true), EDICION(true, true), BAJA(true, false), REACTIVACION(false, true);

        private final boolean activoInicial;
        private final boolean activoSolicitado;

        Escritura(boolean activoInicial, boolean activoSolicitado) {
            this.activoInicial = activoInicial;
            this.activoSolicitado = activoSolicitado;
        }

        MockHttpServletRequestBuilder solicitud(Long id, String nombre) {
            var peticion = this == ALTA ? post(RUTA) : put(RUTA + "/{id}", id);
            String cuerpo = this == ALTA ? """
                    {"nombre":"%s"}
                    """.formatted(nombre) : """
                    {"nombre":"%s","descripcion":"Cambio ficticio","activo":%s}
                    """.formatted(nombre, activoSolicitado);
            return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
    }

    private MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion, String cuerpo) {
        return conCsrfValido(peticion).with(user("admin-ficticio").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private MockHttpServletRequestBuilder conCsrfValido(MockHttpServletRequestBuilder peticion) {
        if (csrfDeSesion == null) {
            return peticion.with(csrf().asHeader());
        }
        var token = csrfDeSesion.generateToken(new MockHttpServletRequest());
        return peticion.cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header(token.getHeaderName(), token.getToken());
    }

    private Long guardar(String nombre, boolean activo) {
        return jdbc.queryForObject("insert into sillon (nombre, activo) values (?, ?) returning id",
                Long.class, nombre, activo);
    }

    private Map<String, Object> datos(Long id) {
        return jdbc.queryForMap("select nombre, descripcion, activo from sillon where id = ?", id);
    }

    private long cantidadConNombre(String nombre) {
        return jdbc.queryForObject("select count(*) from sillon where nombre = ?", Long.class, nombre);
    }
}
