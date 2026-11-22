package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escenarios de la spec administracion-usuarios: cada {@code @DisplayName} es el nombre exacto del escenario.
 * Los dos escenarios del vínculo para otros módulos están en VinculoProfesionalIT. Datos ficticios.
 */
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuariosApiIT extends PruebaIntegracionBase {

    private static final String URL = "/api/usuarios";
    private static final String CLAVE = "clave-fixture-c13";
    private static final long INEXISTENTE = 999_999L;

    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioJpaRepository usuarios;

    /** Otras clases de test pueden dejar administradores confirmados: se desactivan solo dentro de esta transacción. */
    @BeforeEach
    void sinOtrosAdministradores() {
        jdbc.update("update usuario set activo = false where rol = 'ADMIN'");
    }

    // --- Alta de usuario ---

    @Test
    @DisplayName("El administrador da de alta un usuario de recepción")
    void altaRecepcion() throws Exception {
        alta(cuerpo("Recepcion2@Consultorio.Test", "RECEPCION", null))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("recepcion2@consultorio.test"))
                .andExpect(jsonPath("$.nombre").value("Persona Ficticia"))
                .andExpect(jsonPath("$.rol").value("RECEPCION"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.profesionalId").isEmpty());
    }

    @Test
    @DisplayName("El usuario dado de alta puede iniciar sesión con su contraseña")
    void altaPuedeIngresar() throws Exception {
        alta(cuerpo("c13-ingresa@consultorio.test", "RECEPCION", null)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").with(SesionRealPrueba.csrfDe(mockMvc, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"c13-ingresa@consultorio.test\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("c13-ingresa@consultorio.test"))
                .andExpect(jsonPath("$.rol").value("RECEPCION"));
    }

    @Test
    @DisplayName("La contraseña se guarda solo como hash BCrypt")
    void hashBcrypt() throws Exception {
        long id = idDe(alta(cuerpo("c13-hash@consultorio.test", "RECEPCION", null)));

        String guardado = usuarios.findById(id).orElseThrow().getPasswordHash();
        assertThat(guardado).startsWith("$2").isNotEqualTo(CLAVE);
        assertThat(new BCryptPasswordEncoder().matches(CLAVE, guardado)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from usuario where password_hash = ?", Long.class, CLAVE)).isZero();
    }

    @Test
    @DisplayName("Una contraseña fuera de largo responde 400")
    void claveFueraDeLargo() throws Exception {
        for (String clave : new String[] {"corta1", "ñ".repeat(37)}) {
            var datos = cuerpo("c13-larga@consultorio.test", "RECEPCION", null);
            datos.put("password", clave);
            alta(datos)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errores[*].campo").value(hasItem("password")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString(clave))));
        }
    }

    // --- Email único ---

    @Test
    @DisplayName("Un email repetido con otra capitalización responde 409")
    void emailRepetido() throws Exception {
        alta(cuerpo("odonto1-c13@consultorio.test", "RECEPCION", null)).andExpect(status().isCreated());

        alta(cuerpo("ODONTO1-C13@consultorio.test", "RECEPCION", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_EMAIL_DUPLICADO"));
    }

    // --- Vínculo del odontólogo con su profesional ---

    @Test
    @DisplayName("Un odontólogo se da de alta vinculado a su profesional")
    void odontologoVinculado() throws Exception {
        long profesional = profesional("C13-API-1", true);

        alta(cuerpo("c13-odonto@consultorio.test", "ODONTOLOGO", profesional))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profesionalId").value(profesional));
    }

    @Test
    @DisplayName("Un odontólogo sin profesional responde 422")
    void odontologoSinProfesional() throws Exception {
        esperarVinculoInvalido(alta(cuerpo("c13-sinprof@consultorio.test", "ODONTOLOGO", null)));
    }

    @Test
    @DisplayName("Un rol que no es odontólogo con profesional responde 422")
    void otroRolConProfesional() throws Exception {
        esperarVinculoInvalido(alta(cuerpo("c13-recep@consultorio.test", "RECEPCION", profesional("C13-API-2", true))));
    }

    @Test
    @DisplayName("Un profesional inexistente o inactivo responde 422")
    void profesionalInexistenteOInactivo() throws Exception {
        esperarVinculoInvalido(alta(cuerpo("c13-p1@consultorio.test", "ODONTOLOGO", INEXISTENTE)));
        esperarVinculoInvalido(alta(cuerpo("c13-p2@consultorio.test", "ODONTOLOGO", profesional("C13-API-3", false))));
    }

    @Test
    @DisplayName("Un profesional ya vinculado responde 409")
    void profesionalYaVinculado() throws Exception {
        long profesional = profesional("C13-API-4", true);
        alta(cuerpo("c13-v1@consultorio.test", "ODONTOLOGO", profesional)).andExpect(status().isCreated());

        alta(cuerpo("c13-v2@consultorio.test", "ODONTOLOGO", profesional))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PROFESIONAL_YA_VINCULADO"));
    }

    // --- Modificación de usuario ---

    @Test
    @DisplayName("El administrador cambia el rol de un usuario")
    void cambiaRol() throws Exception {
        long id = idDe(alta(cuerpo("c13-rol@consultorio.test", "RECEPCION", null)));

        modificar(id, "c13-rol@consultorio.test", "ADMIN", null, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("Modificar un usuario inexistente responde 404")
    void modificarInexistente() throws Exception {
        esperarNoEncontrado(modificar(INEXISTENTE, "c13-nadie@consultorio.test", "RECEPCION", null, true));
    }

    // --- Desactivación de usuario ---

    @Test
    @DisplayName("Un usuario desactivado no puede iniciar sesión")
    void desactivadoNoIngresa() throws Exception {
        long id = idDe(alta(cuerpo("c13-baja@consultorio.test", "RECEPCION", null)));

        mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null)))
                .andExpect(status().isNoContent());

        var csrf = SesionRealPrueba.csrfDe(mockMvc, null);
        mockMvc.perform(post("/api/auth/login").with(csrf).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"c13-baja@consultorio.test\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(
                        "No pudimos iniciar sesión. Revisá tus credenciales o volvé a intentar más tarde."));
    }

    @Test
    @DisplayName("Repetir la desactivación responde 204")
    void repetirDesactivacion() throws Exception {
        long id = idDe(alta(cuerpo("c13-baja2@consultorio.test", "RECEPCION", null)));
        mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null))).andExpect(status().isNoContent());

        mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null))).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Un usuario reactivado vuelve a iniciar sesión")
    void reactivado() throws Exception {
        long id = idDe(alta(cuerpo("c13-vuelve@consultorio.test", "RECEPCION", null)));
        mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null))).andExpect(status().isNoContent());

        modificar(id, "c13-vuelve@consultorio.test", "RECEPCION", null, true).andExpect(status().isOk());

        SesionRealPrueba.iniciar(mockMvc, "c13-vuelve@consultorio.test", CLAVE);
    }

    @Test
    @DisplayName("Desactivar un usuario inexistente responde 404")
    void desactivarInexistente() throws Exception {
        esperarNoEncontrado(mockMvc.perform(delete(URL + "/" + INEXISTENTE).with(SesionRealPrueba.csrfDe(mockMvc, null))));
    }

    // --- Siempre queda un administrador activo ---

    @Test
    @DisplayName("No se puede desactivar al último administrador activo")
    void ultimoAdministrador() throws Exception {
        long id = idDe(alta(cuerpo("c13-unico-admin@consultorio.test", "ADMIN", null)));

        mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_ULTIMO_ADMIN"));
        modificar(id, "c13-unico-admin@consultorio.test", "RECEPCION", null, true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_ULTIMO_ADMIN"));

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    // --- Consulta de usuarios sin credenciales ---

    @Test
    @DisplayName("Nunca se devuelve el hash de la contraseña")
    void nuncaElHash() throws Exception {
        long id = idDe(alta(cuerpo("c13-sinhash@consultorio.test", "RECEPCION", null)));

        String listado = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String detalle = mockMvc.perform(get(URL + "/" + id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Set<String> permitidos = Set.of("id", "email", "nombre", "rol", "profesionalId", "activo");
        for (JsonNode usuario : json.readTree(listado)) {
            assertThat(usuario.properties()).extracting(Map.Entry::getKey).allMatch(permitidos::contains);
        }
        assertThat(json.readTree(detalle).properties()).extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrderElementsOf(permitidos);
        for (String prohibido : new String[] {"password", "passwordHash", "intentosFallidos", "bloqueadoHasta", "$2"}) {
            assertThat(listado).doesNotContain(prohibido);
            assertThat(detalle).doesNotContain(prohibido);
        }
    }

    @Test
    @DisplayName("El listado se ordena por email")
    void listadoOrdenado() throws Exception {
        alta(cuerpo("c13-b@consultorio.test", "RECEPCION", null)).andExpect(status().isCreated());
        alta(cuerpo("c13-a@consultorio.test", "RECEPCION", null)).andExpect(status().isCreated());

        String listado = mockMvc.perform(get(URL)).andReturn().getResponse().getContentAsString();
        assertThat(listado.indexOf("c13-a@consultorio.test")).isLessThan(listado.indexOf("c13-b@consultorio.test"));
    }

    @Test
    @DisplayName("Consultar un usuario inexistente responde 404")
    void consultarInexistente() throws Exception {
        esperarNoEncontrado(mockMvc.perform(get(URL + "/" + INEXISTENTE)));
    }

    // --- Solo el administrador gestiona usuarios ---

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Recepción no accede a la gestión de usuarios")
    void recepcionNoAccede() throws Exception {
        esperarTodoProhibido();
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("El odontólogo no accede a la gestión de usuarios")
    void odontologoNoAccede() throws Exception {
        esperarTodoProhibido();
    }

    @Test
    @DisplayName("Sin sesión la gestión de usuarios responde 401")
    void sinSesion() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mockMvc.perform(get(URL).with(org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized());
    }

    // --- Ayudas ---

    private Map<String, Object> cuerpo(String email, String rol, Long profesionalId) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("email", email);
        datos.put("nombre", "Persona Ficticia");
        datos.put("rol", rol);
        datos.put("password", CLAVE);
        datos.put("profesionalId", profesionalId);
        return datos;
    }

    private ResultActions alta(Map<String, Object> datos) throws Exception {
        return mockMvc.perform(post(URL).with(SesionRealPrueba.csrfDe(mockMvc, null))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)));
    }

    private ResultActions modificar(long id, String email, String rol, Long profesionalId, boolean activo) throws Exception {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("email", email);
        datos.put("nombre", "Persona Ficticia");
        datos.put("rol", rol);
        datos.put("profesionalId", profesionalId);
        datos.put("activo", activo);
        return mockMvc.perform(put(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(datos)));
    }

    private long idDe(ResultActions respuesta) throws Exception {
        String cuerpo = respuesta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo).get("id").asLong();
    }

    private long profesional(String matricula, boolean activo) {
        return jdbc.queryForObject("insert into profesional (nombre, apellido, matricula, activo) values (?, ?, ?, ?) returning id",
                Long.class, "Profesional", "Ficticio", matricula, activo);
    }

    private static void esperarVinculoInvalido(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("USUARIO_VINCULO_INVALIDO"));
    }

    private static void esperarNoEncontrado(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_ENCONTRADO"));
    }

    /** Un usuario real existente comprueba que ni la baja ni la modificación cambian nada. */
    private void esperarTodoProhibido() throws Exception {
        long id = jdbc.queryForObject("""
                insert into usuario (email, password_hash, nombre, rol, activo, intentos_fallidos, creado_en)
                values ('c13-objetivo@consultorio.test', ?, 'Objetivo', 'RECEPCION', true, 0, now()) returning id
                """, Long.class, new BCryptPasswordEncoder().encode(CLAVE));

        for (ResultActions respuesta : new ResultActions[] {
                mockMvc.perform(get(URL)),
                mockMvc.perform(get(URL + "/" + id)),
                alta(cuerpo("c13-intruso@consultorio.test", "ADMIN", null)),
                modificar(id, "c13-objetivo@consultorio.test", "ADMIN", null, true),
                mockMvc.perform(delete(URL + "/" + id).with(SesionRealPrueba.csrfDe(mockMvc, null)))}) {
            respuesta.andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(content().string(org.hamcrest.Matchers.not(containsString("c13-objetivo"))));
        }
        assertThat(jdbc.queryForObject("select rol || ':' || activo from usuario where id = ?", String.class, id))
                .isEqualTo("RECEPCION:true");
        assertThat(usuarios.existsByEmail("c13-intruso@consultorio.test")).isFalse();
    }
}
