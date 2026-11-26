package ar.edu.utn.turnos.seguridad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Matriz de permisos de KB 03 §RBAC (C-51), como caja negra: la tabla dice qué roles pueden usar cada ruta del
 * personal y la prueba lo compara con lo que responde la API. Las rutas se toman del registro de Spring MVC, así que
 * una ruta nueva sin fila hace fallar la prueba. Lo permitido solo exige "ni 401 ni 403": el cuerpo vacío y el id
 * inexistente pueden dar 400, 404 o 422, y eso lo cubren las pruebas de cada módulo. Las filas "propios" del
 * odontólogo (sus horarios, bloqueos, su profesional y los pacientes de sus turnos) dependen del vínculo y las
 * cubren ProfesionalesTitularidadIT, HorariosApiIT, BloqueosApiIT y PacienteFichaOdontologoIT.
 */
@Transactional
class MatrizPermisosIT extends PruebaIntegracionBase {
    private static final String ID = "999999";
    /** Rutas del personal que no llevan fila: autenticación, que se prueba en AutenticacionPersonalIT. */
    private static final Set<String> SIN_FILA = Set.of(
            "POST /api/auth/login", "POST /api/auth/logout", "GET /api/auth/csrf");
    private static final List<String> TABLAS = List.of("turno", "pacientes", "profesional", "sillon", "prestacion",
            "profesional_prestacion", "horario_atencion", "bloqueo", "lista_espera", "usuario", "configuracion");

    enum Rol { ODONTOLOGO, RECEPCION, ADMIN }

    private static final Set<Rol> TODOS = EnumSet.allOf(Rol.class);
    private static final Set<Rol> PERSONAL = EnumSet.of(Rol.RECEPCION, Rol.ADMIN);
    private static final Set<Rol> SOLO_ADMIN = EnumSet.of(Rol.ADMIN);

    /** Fila de la matriz: quién puede y si para el odontólogo depende de que el recurso sea propio. */
    record Fila(Set<Rol> permitidos, boolean odontologoPropios) {
    }

    private static Fila fila(Set<Rol> permitidos) {
        return new Fila(permitidos, false);
    }

    private static Fila propios(Set<Rol> permitidos) {
        return new Fila(permitidos, true);
    }

    /** KB 03 §RBAC. La clave es "MÉTODO patrón", tal como la registra Spring MVC. */
    static final Map<String, Fila> MATRIZ = new LinkedHashMap<>();

    static {
        MATRIZ.put("GET /api/auth/me", fila(TODOS));
        // Turnos: todos ven la agenda; crear, reprogramar, cancelar y asistencia, recepción y admin.
        MATRIZ.put("GET /api/agenda", fila(TODOS));
        // La disponibilidad sirve para dar turnos: el odontólogo no la consulta (agenda-disponibilidad).
        MATRIZ.put("GET /api/agenda/disponibilidad", fila(PERSONAL));
        MATRIZ.put("POST /api/turnos", fila(PERSONAL));
        MATRIZ.put("POST /api/turnos/{id}/cancelar", fila(PERSONAL));
        MATRIZ.put("POST /api/turnos/{id}/reprogramar", fila(PERSONAL));
        MATRIZ.put("POST /api/turnos/{id}/asistencia", fila(PERSONAL));
        // Pacientes: el odontólogo solo lee la ficha de los pacientes de sus turnos; la baja es del admin.
        MATRIZ.put("GET /api/pacientes", fila(PERSONAL));
        MATRIZ.put("GET /api/pacientes/{id}", propios(TODOS));
        MATRIZ.put("POST /api/pacientes", fila(PERSONAL));
        MATRIZ.put("PUT /api/pacientes/{id}", fila(PERSONAL));
        MATRIZ.put("DELETE /api/pacientes/{id}", fila(SOLO_ADMIN));
        // Horarios y bloqueos: el odontólogo lee los propios; recepción y admin, todo.
        MATRIZ.put("GET /api/profesionales/{id}/horarios", propios(TODOS));
        MATRIZ.put("POST /api/profesionales/{id}/horarios", fila(PERSONAL));
        MATRIZ.put("PUT /api/profesionales/{id}/horarios/{horarioId}", fila(PERSONAL));
        MATRIZ.put("DELETE /api/profesionales/{id}/horarios/{horarioId}", fila(PERSONAL));
        MATRIZ.put("GET /api/profesionales/{id}/bloqueos", propios(TODOS));
        MATRIZ.put("POST /api/profesionales/{id}/bloqueos", fila(PERSONAL));
        MATRIZ.put("DELETE /api/profesionales/{id}/bloqueos/{bloqueoId}", fila(PERSONAL));
        // Profesionales, sillones y prestaciones: todos leen; solo el admin escribe.
        MATRIZ.put("GET /api/profesionales", fila(TODOS));
        MATRIZ.put("GET /api/profesionales/{id}", propios(TODOS));
        MATRIZ.put("POST /api/profesionales", fila(SOLO_ADMIN));
        MATRIZ.put("PUT /api/profesionales/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/profesionales/{id}/prestaciones", propios(TODOS));
        MATRIZ.put("PUT /api/profesionales/{id}/prestaciones", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/sillones", fila(TODOS));
        MATRIZ.put("GET /api/sillones/{id}", fila(TODOS));
        MATRIZ.put("POST /api/sillones", fila(SOLO_ADMIN));
        MATRIZ.put("PUT /api/sillones/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/prestaciones", fila(TODOS));
        MATRIZ.put("GET /api/prestaciones/{id}", fila(TODOS));
        MATRIZ.put("POST /api/prestaciones", fila(SOLO_ADMIN));
        MATRIZ.put("PUT /api/prestaciones/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("DELETE /api/prestaciones/{id}", fila(SOLO_ADMIN));
        // Lista de espera: todos leen; recepción y admin anotan, asignan y dan de baja.
        MATRIZ.put("GET /api/lista-espera", fila(TODOS));
        MATRIZ.put("GET /api/lista-espera/{id}", fila(TODOS));
        MATRIZ.put("GET /api/lista-espera/compatibles", fila(PERSONAL));
        MATRIZ.put("POST /api/lista-espera", fila(PERSONAL));
        MATRIZ.put("POST /api/lista-espera/{id}/asignar", fila(PERSONAL));
        MATRIZ.put("DELETE /api/lista-espera/{id}", fila(PERSONAL));
        // Usuarios: solo el admin. Configuración: recepción lee, el admin modifica. Notificaciones: recepción y admin.
        MATRIZ.put("GET /api/usuarios", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/usuarios/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("POST /api/usuarios", fila(SOLO_ADMIN));
        MATRIZ.put("PUT /api/usuarios/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("DELETE /api/usuarios/{id}", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/configuracion", fila(PERSONAL));
        MATRIZ.put("PUT /api/configuracion", fila(SOLO_ADMIN));
        MATRIZ.put("GET /api/notificaciones", fila(PERSONAL));
        MATRIZ.put("GET /api/notificaciones/{id}/vista-previa", fila(PERSONAL));
    }

    /**
     * Cuerpos válidos para las escrituras de catálogo: con "{}" la validación responde 400 antes que el permiso, y
     * la prueba tiene que llegar al chequeo de rol.
     */
    private static final Map<String, String> CUERPOS = Map.of(
            "POST /api/profesionales", """
                    {"nombre":"Ficticia","apellido":"Matriz","matricula":"MP-MATRIZ","especialidad":"General"}""",
            "PUT /api/profesionales/{id}", """
                    {"nombre":"Ficticia","apellido":"Matriz","matricula":"MP-MATRIZ","especialidad":"General","activo":true}""",
            "PUT /api/profesionales/{id}/prestaciones", "{\"prestacionIds\":[]}",
            "POST /api/sillones", "{\"nombre\":\"Sillón Matriz\"}",
            "PUT /api/sillones/{id}", "{\"nombre\":\"Sillón Matriz\",\"activo\":true}",
            "POST /api/prestaciones", "{\"nombre\":\"Prestación Matriz\",\"duracionDefaultMin\":30}",
            "PUT /api/prestaciones/{id}", "{\"nombre\":\"Prestación Matriz\",\"duracionDefaultMin\":30,\"activa\":true}");

    /**
     * Parámetros válidos para las consultas que validan antes de autorizar (por ejemplo, la disponibilidad): sin
     * ellos, un 400 taparía el 403 y la prueba daría por permitida una ruta denegada.
     */
    private static final Map<String, Map<String, String>> PARAMETROS = Map.of(
            "GET /api/agenda", Map.of("vista", "dia", "fecha", "2026-12-07"),
            "GET /api/agenda/disponibilidad", Map.of("profesionalId", ID, "prestacionId", ID,
                    "desde", "2030-06-10T09:00:00-03:00", "hasta", "2030-06-10T10:00:00-03:00"),
            "GET /api/lista-espera/compatibles", Map.of("profesionalId", ID,
                    "inicio", "2030-06-10T09:00:00-03:00", "fin", "2030-06-10T10:00:00-03:00"));

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private JdbcTemplate jdbc;

    /** "MÉTODO patrón" de las rutas del personal registradas en Spring MVC (fuera de la zona pública). */
    private Set<String> rutasDelPersonal() {
        Set<String> rutas = new TreeSet<>();
        mappings.getHandlerMethods().keySet().forEach(info -> {
            for (String patron : info.getPatternValues()) {
                if (patron.startsWith("/api/") && !patron.startsWith("/api/public/")) {
                    info.getMethodsCondition().getMethods().forEach(m -> rutas.add(m.name() + " " + patron));
                }
            }
        });
        rutas.removeAll(SIN_FILA);
        return rutas;
    }

    private MockHttpServletRequestBuilder pedido(String ruta) {
        String[] partes = ruta.split(" ", 2);
        String url = partes[1].replaceAll("\\{[^}]+}", ID);
        var pedido = request(HttpMethod.valueOf(partes[0]), url).contentType(MediaType.APPLICATION_JSON)
                .content(CUERPOS.getOrDefault(ruta, "{}"));
        PARAMETROS.getOrDefault(ruta, Map.of()).forEach(pedido::param);
        return pedido;
    }

    /**
     * Recepción y admin con un usuario de prueba; el odontólogo, con una sesión real vinculada a un profesional
     * ficticio, porque sin vínculo el sistema le niega la agenda (sus lecturas dependen de su profesional).
     */
    private MockHttpServletResponse comoRol(String ruta, Rol rol) throws Exception {
        RequestPostProcessor quien = rol == Rol.ODONTOLOGO
                ? odontologoVinculado()
                : user(rol.name().toLowerCase() + "-ficticio").roles(rol.name());
        return mockMvc.perform(pedido(ruta).with(quien).with(csrfValido())).andReturn().getResponse();
    }

    private RequestPostProcessor odontologo;

    private RequestPostProcessor odontologoVinculado() {
        if (odontologo == null) {
            Long profesional = DatosAgenda.profesional(jdbc, true);
            String email = "odontologo-matriz@consultorio.test";
            Long usuario = jdbc.queryForObject("""
                    insert into usuario (email, password_hash, nombre, rol, profesional_id, activo, intentos_fallidos,
                                         creado_en)
                    values (?, ?, 'Odontólogo Ficticio', 'ODONTOLOGO', ?, true, 0, now()) returning id
                    """, Long.class, email, "$2a$10$" + "x".repeat(53), profesional);
            var identidad = new IdentidadPersonal(usuario, email, ar.edu.utn.turnos.seguridad.domain.Rol.ODONTOLOGO);
            odontologo = authentication(new UsernamePasswordAuthenticationToken(identidad, null,
                    List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO"))));
        }
        return odontologo;
    }

    private Map<String, Long> filasPorTabla() {
        Map<String, Long> filas = new LinkedHashMap<>();
        TABLAS.forEach(tabla -> filas.put(tabla, jdbc.queryForObject("select count(*) from " + tabla, Long.class)));
        return filas;
    }

    /** Resultados que no coinciden con la matriz para las filas elegidas, como "MÉTODO ruta [ROL] → estado". */
    private List<String> diferencias(Set<Rol> roles, java.util.function.Predicate<String> rutas) throws Exception {
        List<String> distintos = new ArrayList<>();
        for (var entrada : MATRIZ.entrySet()) {
            if (!rutas.test(entrada.getKey())) {
                continue;
            }
            Fila fila = entrada.getValue();
            for (Rol rol : roles) {
                if (rol == Rol.ODONTOLOGO && fila.odontologoPropios()) {
                    continue;
                }
                MockHttpServletResponse respuesta = comoRol(entrada.getKey(), rol);
                int estado = respuesta.getStatus();
                boolean permitido = fila.permitidos().contains(rol);
                boolean coincide = permitido
                        ? estado != 401 && estado != 403
                        : estado == 403 && respuesta.getContentType() != null
                                && respuesta.getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                if (!coincide) {
                    distintos.add(entrada.getKey() + " [" + rol + "] → " + estado);
                }
            }
        }
        return distintos;
    }

    @Test
    @DisplayName("Una ruta sin fila en la matriz hace fallar la verificación")
    void unaRutaSinFilaHaceFallarLaVerificacion() {
        Set<String> rutas = rutasDelPersonal();
        assertThat(rutas).as("la prueba no puede pasar vacía").contains("POST /api/turnos", "GET /api/usuarios");

        Set<String> sinFila = new TreeSet<>(rutas);
        sinFila.removeAll(MATRIZ.keySet());
        Set<String> filasSinRuta = new TreeSet<>(MATRIZ.keySet());
        filasSinRuta.removeAll(rutas);

        assertThat(sinFila).as("rutas del personal sin fila en la matriz de permisos").isEmpty();
        assertThat(filasSinRuta).as("filas de la matriz sin ruta registrada").isEmpty();
    }

    @Test
    @DisplayName("Recepción no administra usuarios ni modifica la configuración")
    void recepcionNoAdministraUsuariosNiModificaLaConfiguracion() throws Exception {
        Map<String, Long> antes = filasPorTabla();
        assertThat(diferencias(EnumSet.of(Rol.RECEPCION),
                ruta -> ruta.contains("/api/usuarios") || ruta.equals("PUT /api/configuracion"))).isEmpty();
        assertThat(filasPorTabla()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Recepción no modifica los catálogos")
    void recepcionNoModificaLosCatalogos() throws Exception {
        Map<String, Long> antes = filasPorTabla();
        assertThat(diferencias(EnumSet.of(Rol.RECEPCION), ruta -> !ruta.startsWith("GET")
                && (ruta.matches("\\S+ /api/(sillones|prestaciones).*")
                        || ruta.matches("\\S+ /api/profesionales(/\\{id})?")
                        || ruta.endsWith("/api/profesionales/{id}/prestaciones")))).isEmpty();
        assertThat(filasPorTabla()).isEqualTo(antes);
    }

    @Test
    @DisplayName("El odontólogo no lee usuarios, configuración ni el registro de notificaciones")
    void elOdontologoNoLeeUsuariosConfiguracionNiNotificaciones() throws Exception {
        assertThat(diferencias(EnumSet.of(Rol.ODONTOLOGO), ruta -> ruta.startsWith("GET ")
                && (ruta.contains("/api/usuarios") || ruta.contains("/api/configuracion")
                        || ruta.contains("/api/notificaciones")))).isEmpty();
    }

    @Test
    @DisplayName("Cada rol lee lo que la matriz le permite")
    void cadaRolLeeLoQueLaMatrizLePermite() throws Exception {
        assertThat(diferencias(TODOS, ruta -> true)).as("toda la matriz, por rol").isEmpty();
    }

    @Test
    @DisplayName("Toda ruta del personal sin sesión responde 401")
    void todaRutaDelPersonalSinSesionResponde401() throws Exception {
        List<String> sin401 = new ArrayList<>();
        for (String ruta : rutasDelPersonal()) {
            MockHttpServletResponse respuesta = mockMvc.perform(pedido(ruta).with(csrfValido()))
                    .andReturn().getResponse();
            boolean problema = respuesta.getContentType() != null
                    && respuesta.getContentType().startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            if (respuesta.getStatus() != 401 || !problema) {
                sin401.add(ruta + " → " + respuesta.getStatus());
            }
        }
        assertThat(sin401).as("rutas del personal que no responden 401 sin sesión").isEmpty();
    }
}
