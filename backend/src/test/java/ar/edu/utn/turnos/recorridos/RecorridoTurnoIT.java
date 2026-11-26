package ar.edu.utn.turnos.recorridos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.notificacion.application.PlanificadorRecordatorios;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Recorridos del turno de punta a punta entre módulos (C-51): reserva online, agenda, cancelación y
 * reprogramación, lista de espera, toma del hueco, emails y recordatorio. Todo por HTTP, con PostgreSQL real y
 * transacciones confirmadas, porque los emails y los avisos salen después del commit. Los tokens del turno y de
 * la oferta se leen del email capturado, como haría el paciente. En el perfil {@code test} el aviso a la lista de
 * espera es sincrónico, así que al volver la cancelación o la reprogramación las ofertas ya están.
 *
 * <p>Reloj fijo: martes 1/12/2026 09:00 de Buenos Aires (12:00Z). El profesional atiende lunes y viernes de 08:00
 * a 13:00. Los datos son ficticios y propios de cada prueba, que borra lo que creó.
 */
@Import(RelojFijoConfig.class)
class RecorridoTurnoIT extends PruebaIntegracionBase {
    private static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;
    /** Viernes 4/12/2026 09:00 de Buenos Aires. */
    private static final Instant VIERNES_9 = Instant.parse("2026-12-04T12:00:00Z");
    /** Lunes 7/12/2026 09:00 de Buenos Aires. */
    private static final Instant LUNES_9 = Instant.parse("2026-12-07T12:00:00Z");
    private static final Pattern ENLACE_TURNO = Pattern.compile("/turno/([A-Za-z0-9_-]{43})");
    private static final Pattern ENLACE_OFERTA = Pattern.compile("/oferta/([A-Za-z0-9_-]{43})");
    private static final AtomicInteger IPS = new AtomicInteger();

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private InMemoryNotificacionAdapter correo;
    @Autowired private PlanificadorRecordatorios planificador;

    private Long profesional;
    private Long sillon;
    private Long consulta;
    private Long recepcionista;
    private RequestPostProcessor comoRecepcion;
    private List<Long> sillonesAjenos;
    private final List<Long> pacientes = new ArrayList<>();
    private final List<Long> entradas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        correo.limpiar();
        // Con un solo sillón activo, la elección del sillón en la reserva y en la toma es determinista.
        sillonesAjenos = jdbc.queryForList("select id from sillon where activo", Long.class);
        jdbc.update("update sillon set activo = false where activo");
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        consulta = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, consulta);
        DatosAgenda.horario(jdbc, profesional, 1, "08:00", "13:00");
        DatosAgenda.horario(jdbc, profesional, 5, "08:00", "13:00");
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        recepcionista = (Long) usuario[0];
        comoRecepcion = authentication(new UsernamePasswordAuthenticationToken(
                new IdentidadPersonal(recepcionista, (String) usuario[1], Rol.RECEPCION), null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPCION"))));
    }

    @AfterEach
    void limpiar() {
        var turnos = "(select id from turno where profesional_id = ?)";
        pacientes.addAll(jdbc.queryForList("select distinct paciente_id from turno where profesional_id = ?",
                Long.class, profesional));
        jdbc.update("delete from notificacion_enviada where turno_id in " + turnos, profesional);
        jdbc.update("""
                delete from notificacion_enviada
                where oferta_hueco_id in (select id from oferta_hueco where profesional_id = ?)
                """, profesional);
        jdbc.update("delete from oferta_hueco where profesional_id = ?", profesional);
        jdbc.update("delete from token_turno where turno_id in " + turnos, profesional);
        jdbc.update("delete from consentimiento_turno where turno_id in " + turnos, profesional);
        entradas.forEach(id -> jdbc.update("delete from lista_espera where id = ?", id));
        jdbc.update("delete from turno where profesional_id = ?", profesional);
        pacientes.stream().distinct().forEach(id -> {
            jdbc.update("delete from notificacion_enviada where paciente_id = ?", id);
            jdbc.update("delete from pacientes where id = ?", id);
        });
        DatosAgenda.limpiar(jdbc, -1L, new Long[] {profesional}, new Long[] {sillon}, new Long[] {consulta});
        jdbc.update("delete from usuario where id = ?", recepcionista);
        sillonesAjenos.forEach(id -> jdbc.update("update sillon set activo = true where id = ?", id));
        correo.limpiar();
    }

    // ---------- Recorridos ----------

    @Test
    @DisplayName("Un paciente reserva online y gestiona su turno por el enlace")
    void unPacienteReservaOnlineYGestionaSuTurnoPorElEnlace() throws Exception {
        String email = "reserva-" + unico() + "@recorrido.test";

        publico(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON).content(reserva(email)))
                .andExpect(status().isCreated());

        Long turno = turnoEn(LUNES_9);
        mockMvc.perform(get("/api/agenda").with(comoRecepcion).param("vista", "dia").param("fecha", "2026-12-07")
                        .param("profesionalId", profesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[*].id").value(Matchers.contains(turno.intValue())));
        var confirmaciones = enviados(email, "Confirmación de turno odontológico");
        assertThat(confirmaciones).hasSize(1);
        assertThat(correo.enviados()).hasSize(1);

        String token = token(confirmaciones.get(0), ENLACE_TURNO);
        publico(get("/api/public/turnos/{token}", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"));
        publico(post("/api/public/turnos/{token}/cancelar", token)).andExpect(status().isNoContent());
        assertThat(estado(turno)).isEqualTo("CANCELADO");
        publico(get("/api/public/turnos/{token}", token)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Una cancelación termina con el hueco tomado desde la lista de espera")
    void unaCancelacionTerminaConElHuecoTomadoDesdeLaListaDeEspera() throws Exception {
        Long cancelado = paciente("cancelado@recorrido.test");
        Long interesado = paciente("interesado@recorrido.test");
        Long entrada = entrada(interesado);
        Long turno = turno(cancelado, VIERNES_9);

        mockMvc.perform(post("/api/turnos/{id}/cancelar", turno).with(comoRecepcion).with(csrfValido()))
                .andExpect(status().is2xxSuccessful());

        assertThat(enviados(email(cancelado), "Cancelación de turno")).hasSize(1);
        tomarHueco(interesado, entrada);
    }

    @Test
    @DisplayName("Una reprogramación termina con el hueco tomado desde la lista de espera")
    void unaReprogramacionTerminaConElHuecoTomadoDesdeLaListaDeEspera() throws Exception {
        Long reprogramado = paciente("reprogramado@recorrido.test");
        Long interesado = paciente("interesado@recorrido.test");
        Long entrada = entrada(interesado);
        Long turno = turno(reprogramado, VIERNES_9);

        mockMvc.perform(post("/api/turnos/{id}/reprogramar", turno).with(comoRecepcion).with(csrfValido())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inicio\":\"" + LUNES_9 + "\"}"))
                .andExpect(status().isOk());

        assertThat(enviados(email(reprogramado), "Reprogramación de turno")).hasSize(1);
        assertThat(turnoEn(LUNES_9)).isEqualTo(turno);
        tomarHueco(interesado, entrada);
    }

    @Test
    @DisplayName("El recordatorio llega una sola vez y no llega al turno cancelado")
    void elRecordatorioLlegaUnaSolaVezYNoLlegaAlTurnoCancelado() throws Exception {
        Long vigente = paciente("vigente@recorrido.test");
        Long cancelado = paciente("cancelado@recorrido.test");
        turno(vigente, AHORA.plus(Duration.ofHours(2)));
        Long aCancelar = turno(cancelado, AHORA.plus(Duration.ofHours(3)));
        mockMvc.perform(post("/api/turnos/{id}/cancelar", aCancelar).with(comoRecepcion).with(csrfValido()))
                .andExpect(status().is2xxSuccessful());

        planificador.procesar();
        planificador.procesar();

        var recordatorios = correo.enviados().stream()
                .filter(m -> m.asunto().startsWith("Recordatorio de turno")).toList();
        assertThat(recordatorios).extracting(MensajeNotificacion::destinatario).containsExactly(email(vigente));
    }

    // ---------- Pasos compartidos ----------

    /**
     * El interesado recibe el aviso del hueco del viernes a las 09:00, lo toma con el token del email y queda con
     * un turno nuevo en ese horario, con su email de confirmación; la entrada pasa a ASIGNADA.
     */
    private void tomarHueco(Long interesado, Long entrada) throws Exception {
        var avisos = enviados(email(interesado), "Hueco disponible");
        assertThat(avisos).hasSize(1);

        publico(post("/api/public/ofertas/{token}/tomar", token(avisos.get(0), ENLACE_OFERTA)))
                .andExpect(status().isCreated());

        Long nuevo = turnoEn(VIERNES_9);
        assertThat(jdbc.queryForObject("select paciente_id from turno where id = ?", Long.class, nuevo))
                .isEqualTo(interesado);
        assertThat(estado(nuevo)).isEqualTo("PROGRAMADO");
        var confirmaciones = enviados(email(interesado), "Turno confirmado desde la lista de espera");
        assertThat(confirmaciones).hasSize(1);
        token(confirmaciones.get(0), ENLACE_TURNO);
        assertThat(jdbc.queryForMap("select estado, turno_asignado_id from lista_espera where id = ?", entrada))
                .containsEntry("estado", "ASIGNADA").containsEntry("turno_asignado_id", nuevo);
    }

    // ---------- Datos ----------

    private String reserva(String email) {
        var cuerpo = json.createObjectNode();
        cuerpo.put("prestacionId", consulta);
        cuerpo.put("profesionalId", profesional);
        cuerpo.put("inicio", LUNES_9.toString());
        cuerpo.put("nombre", "Ana");
        cuerpo.put("apellido", "Ficticia");
        cuerpo.put("email", email);
        cuerpo.put("telefono", "11 5555-0051");
        cuerpo.put("obraSocial", "Obra Social Ficticia");
        cuerpo.put("aceptaTratamientoDatos", true);
        cuerpo.put("versionConsentimiento", "2026-11-v1");
        return cuerpo.toString();
    }

    private Long paciente(String email) {
        Long id = DatosFicticios.paciente(jdbc, email);
        pacientes.add(id);
        return id;
    }

    private Long entrada(Long paciente) {
        Long id = DatosFicticios.entrada(jdbc, paciente, consulta, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", "2026-11-01T10:00:00Z");
        entradas.add(id);
        return id;
    }

    private Long turno(Long paciente, Instant inicio) {
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, consulta, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), "PROGRAMADO");
    }

    /** Turno no cancelado del profesional que empieza en ese instante. */
    private Long turnoEn(Instant inicio) {
        return jdbc.queryForObject("""
                select id from turno where profesional_id = ? and inicio = ? and estado <> 'CANCELADO'
                """, Long.class, profesional, OffsetDateTime.ofInstant(inicio, java.time.ZoneOffset.UTC));
    }

    private String estado(Long turno) {
        return jdbc.queryForObject("select estado from turno where id = ?", String.class, turno);
    }

    private String email(Long paciente) {
        return jdbc.queryForObject("select email from pacientes where id = ?", String.class, paciente);
    }

    private List<MensajeNotificacion> enviados(String destinatario, String asunto) {
        return correo.enviados().stream()
                .filter(m -> destinatario.equalsIgnoreCase(m.destinatario()) && m.asunto().startsWith(asunto))
                .toList();
    }

    private static String token(MensajeNotificacion mensaje, Pattern enlace) {
        var m = enlace.matcher(mensaje.cuerpo());
        assertThat(m.find()).as("el email lleva un enlace usable").isTrue();
        return m.group(1);
    }

    /** Pedido público desde una IP distinta, para no chocar con el límite de tasa (C-17). */
    private ResultActions publico(MockHttpServletRequestBuilder pedido) throws Exception {
        int n = IPS.incrementAndGet();
        return mockMvc.perform(pedido.with(solicitud -> {
            solicitud.setRemoteAddr("10.51." + (n / 250) + "." + (n % 250 + 1));
            return solicitud;
        }));
    }

    private static String unico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
