package ar.edu.utn.turnos.shared.notificacion.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Consulta del registro de notificaciones (C-25, US-020). Datos ficticios; cada test arranca con la tabla vacía. */
@Transactional
class ConsultaNotificacionesApiIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/notificaciones";
    private static final String PERSONAL = "personal-ficticio";
    private static final String ERROR_SMTP = "No se pudo enviar la notificación por email";
    private static final Instant INICIO = Instant.parse("2026-12-10T14:30:00Z");
    private static final Instant PROGRAMADA = Instant.parse("2026-12-09T14:30:00Z");
    private static final Instant ENVIO = Instant.parse("2026-12-09T14:30:05Z");
    private static final Instant REINTENTO = Instant.parse("2026-12-09T14:40:00Z");

    @Autowired
    private NotificacionRepositorio repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void vaciar() {
        jdbc.update("delete from notificacion_enviada");
    }

    // --- Datos ---

    private NotificacionEnviada pendiente(TipoNotificacion tipo, Long turnoId) {
        return NotificacionEnviada.pendiente(tipo, "paciente" + turnoId + "@correo.test", turnoId, null, 7L,
                INICIO, PROGRAMADA);
    }

    private Long registrarPendiente(Long turnoId) {
        return repositorio.guardar(pendiente(TipoNotificacion.RECORDATORIO, turnoId)).id();
    }

    private Long registrarEnviada(Long turnoId) {
        return repositorio.guardar(pendiente(TipoNotificacion.CONFIRMACION_RESERVA, turnoId)
                .conEnvioExitoso(ENVIO)).id();
    }

    /** FALLIDA con 2 intentos: el primero falló, se reintentó y volvió a fallar. */
    private Long registrarFallida(Long turnoId, TipoNotificacion tipo) {
        NotificacionEnviada primerFallo = repositorio.guardar(pendiente(tipo, turnoId)
                .conIntentoFallido(ERROR_SMTP, REINTENTO));
        return repositorio.guardar(primerFallo.conIntentoFallido(ERROR_SMTP, REINTENTO)).id();
    }

    private Long registrarOferta() {
        return repositorio.guardar(NotificacionEnviada.pendiente(TipoNotificacion.OFERTA_HUECO,
                "espera@correo.test", null, 900L, 8L, INICIO, PROGRAMADA)).id();
    }

    /** Registra {@code cantidad} pendientes de turnos distintos y devuelve sus ids en orden de registro. */
    private List<Long> registrarPendientes(int cantidad) {
        List<Long> ids = new ArrayList<>();
        for (long turno = 1; turno <= cantidad; turno++) {
            ids.add(registrarPendiente(1000L + turno));
        }
        return ids;
    }

    private static MockHttpServletRequestBuilder consulta(String rol) {
        return get(RUTA).with(user(PERSONAL).roles(rol));
    }

    private static List<Integer> enteros(List<Long> ids) {
        return ids.stream().map(Long::intValue).toList();
    }

    // --- Consulta ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Recepción lista el registro de notificaciones")
    void listaElRegistro(String rol) throws Exception {
        Long pendiente = registrarPendiente(10L);
        Long enviada = registrarEnviada(11L);
        Long fallida = registrarFallida(12L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(consulta(rol))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(contains(fallida.intValue(), enviada.intValue(),
                        pendiente.intValue())))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.contenido[2].id").value(pendiente))
                .andExpect(jsonPath("$.contenido[2].tipo").value("RECORDATORIO"))
                .andExpect(jsonPath("$.contenido[2].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.contenido[2].canal").value("EMAIL"))
                .andExpect(jsonPath("$.contenido[2].destinatario").value("paciente10@correo.test"))
                .andExpect(jsonPath("$.contenido[2].turnoId").value(10))
                .andExpect(jsonPath("$.contenido[2].ofertaHuecoId").value((Object) null))
                .andExpect(jsonPath("$.contenido[2].pacienteId").value(7))
                .andExpect(jsonPath("$.contenido[2].inicioReferencia").value("2026-12-10T14:30:00Z"))
                .andExpect(jsonPath("$.contenido[2].intentos").value(0))
                .andExpect(jsonPath("$.contenido[2].ultimoError").value((Object) null))
                .andExpect(jsonPath("$.contenido[2].programadaPara").value("2026-12-09T14:30:00Z"))
                .andExpect(jsonPath("$.contenido[2].enviadaEn").value((Object) null));
    }

    @Test
    @DisplayName("Sin notificaciones el registro está vacío")
    void registroVacio() throws Exception {
        mockMvc.perform(consulta("RECEPCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    @DisplayName("Una notificación fallida muestra su último error")
    void fallidaMuestraError() throws Exception {
        Long fallida = registrarFallida(12L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(consulta("RECEPCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(fallida))
                .andExpect(jsonPath("$.contenido[0].estado").value("FALLIDA"))
                .andExpect(jsonPath("$.contenido[0].intentos").value(2))
                .andExpect(jsonPath("$.contenido[0].ultimoError").value(ERROR_SMTP))
                .andExpect(jsonPath("$.contenido[0].programadaPara").value("2026-12-09T14:40:00Z"));
    }

    @Test
    @DisplayName("Una notificación enviada no trae error")
    void enviadaSinError() throws Exception {
        registrarEnviada(11L);

        mockMvc.perform(consulta("ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$.contenido[0].ultimoError").value((Object) null))
                .andExpect(jsonPath("$.contenido[0].enviadaEn").value("2026-12-09T14:30:05Z"));
    }

    // --- Filtros ---

    @Test
    @DisplayName("Filtrar por estado devuelve solo las de ese estado")
    void filtroPorEstado() throws Exception {
        registrarPendiente(10L);
        registrarEnviada(11L);
        Long fallida12 = registrarFallida(12L, TipoNotificacion.RECORDATORIO);
        Long fallida13 = registrarFallida(13L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(consulta("RECEPCION").param("estado", "FALLIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(contains(fallida13.intValue(), fallida12.intValue())))
                .andExpect(jsonPath("$.contenido[*].estado").value(contains("FALLIDA", "FALLIDA")))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    @DisplayName("Filtrar por turno devuelve solo las de ese turno")
    void filtroPorTurno() throws Exception {
        Long enviada = registrarEnviada(10L);
        Long recordatorio = registrarPendiente(10L);
        registrarPendiente(20L);
        registrarOferta();

        mockMvc.perform(consulta("RECEPCION").param("turnoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(contains(recordatorio.intValue(), enviada.intValue())))
                .andExpect(jsonPath("$.contenido[*].turnoId").value(contains(10, 10)))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    @DisplayName("Los filtros de estado y turno se combinan")
    void filtrosCombinados() throws Exception {
        registrarEnviada(10L);
        Long fallidaTurno10 = registrarFallida(10L, TipoNotificacion.RECORDATORIO);
        registrarFallida(20L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(consulta("ADMIN").param("estado", "FALLIDA").param("turnoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(contains(fallidaTurno10.intValue())))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("Un turno sin notificaciones devuelve una página vacía")
    void turnoSinNotificaciones() throws Exception {
        registrarPendiente(10L);

        mockMvc.perform(consulta("RECEPCION").param("turnoId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    @DisplayName("Un estado desconocido responde 400")
    void estadoDesconocido() throws Exception {
        mockMvc.perform(consulta("RECEPCION").param("estado", "PERDIDA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // --- Paginación ---

    @Test
    @DisplayName("Sin parámetros se devuelve la primera página de 20")
    void primeraPaginaPorDefecto() throws Exception {
        List<Long> ids = registrarPendientes(25);

        mockMvc.perform(consulta("RECEPCION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(20)))
                .andExpect(jsonPath("$.contenido[0].id").value(ids.get(24)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(20))
                .andExpect(jsonPath("$.totalElementos").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    @DisplayName("La segunda página trae el resto en el mismo orden")
    void segundaPagina() throws Exception {
        List<Long> ids = registrarPendientes(25);
        List<Integer> cincoMasAntiguas = enteros(ids.subList(0, 5).reversed());

        mockMvc.perform(consulta("RECEPCION").param("pagina", "1").param("tamanio", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id").value(contains(cincoMasAntiguas.toArray())))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(25));
    }

    @Test
    @DisplayName("Una página posterior a la última viene vacía")
    void paginaPosteriorALaUltima() throws Exception {
        registrarPendientes(3);

        mockMvc.perform(consulta("RECEPCION").param("pagina", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.pagina").value(5))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @ParameterizedTest
    @CsvSource({"pagina,-1", "tamanio,0", "tamanio,101"})
    @DisplayName("Parámetros de paginación fuera de rango responden 400")
    void paginacionFueraDeRango(String parametro, String valor) throws Exception {
        mockMvc.perform(consulta("RECEPCION").param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // --- Acceso ---

    @Test
    @DisplayName("El odontólogo no puede ver el registro")
    void odontologoRecibe403() throws Exception {
        registrarFallida(12L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(consulta("ODONTOLOGO"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.contenido").doesNotExist());
    }

    @Test
    @DisplayName("Sin sesión el registro responde 401")
    void sinSesionRecibe401() throws Exception {
        registrarFallida(12L, TipoNotificacion.RECORDATORIO);

        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.contenido").doesNotExist());
    }
}
