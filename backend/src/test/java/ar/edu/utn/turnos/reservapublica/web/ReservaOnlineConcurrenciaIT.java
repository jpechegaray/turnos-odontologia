package ar.edu.utn.turnos.reservapublica.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Reserva online con transacciones confirmadas (C-41): 409 con alternativas, carreras con PostgreSQL real
 * (RN-15) y el email de confirmación, que solo sale después del commit. Cada prueba borra lo que creó.
 * Reloj fijo: martes 1/12/2026 12:00Z; el profesional atiende los lunes de 12:00Z a 14:00Z.
 */
@Import(RelojFijoConfig.class)
class ReservaOnlineConcurrenciaIT extends PruebaIntegracionBase {

    private static final String LUNES_9 = "2026-12-07T12:00:00Z";
    private static final Pattern ENLACE = Pattern.compile("/turno/([A-Za-z0-9_-]{43})");
    private static final AtomicInteger IPS = new AtomicInteger();

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired InMemoryNotificacionAdapter correo;
    @Autowired ServicioNotificaciones notificaciones;
    Long profesional;
    Long sillon;
    Long prestacion;
    String sufijo;

    @BeforeEach
    void preparar() {
        correo.limpiar();
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "11:00");
        sufijo = UUID.randomUUID().toString().substring(0, 8) + "@reserva-c41.test";
    }

    @AfterEach
    void limpiar() {
        var turnos = "(select id from turno where profesional_id = ?)";
        jdbc.update("delete from notificacion_enviada where turno_id in " + turnos, profesional);
        jdbc.update("delete from token_turno where turno_id in " + turnos, profesional);
        jdbc.update("delete from consentimiento_turno where turno_id in " + turnos, profesional);
        var pacientes = jdbc.queryForList("select distinct paciente_id from turno where profesional_id = ?",
                Long.class, profesional);
        jdbc.update("delete from turno where profesional_id = ?", profesional);
        for (var paciente : pacientes) {
            jdbc.update("delete from turno where paciente_id = ?", paciente);
        }
        jdbc.update("delete from pacientes where email like ?", "%" + sufijo);
        DatosAgenda.limpiar(jdbc, -1L, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        correo.limpiar();
    }

    // ---------- Ayudantes ----------

    ObjectNode reserva(String inicio, String email) {
        var cuerpo = json.createObjectNode();
        cuerpo.put("prestacionId", prestacion);
        cuerpo.put("profesionalId", profesional);
        cuerpo.put("inicio", inicio);
        cuerpo.put("nombre", "Ana");
        cuerpo.put("apellido", "Ficticia");
        cuerpo.put("email", email);
        cuerpo.put("telefono", "1155550001");
        cuerpo.put("obraSocial", "Obra Social Ficticia");
        cuerpo.put("aceptaTratamientoDatos", true);
        cuerpo.put("versionConsentimiento", "2026-11-v1");
        return cuerpo;
    }

    ResultActions reservar(ObjectNode cuerpo) throws Exception {
        int n = IPS.incrementAndGet();
        return mockMvc.perform(post("/api/public/reservas").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.toString()).with(solicitud -> {
                    solicitud.setRemoteAddr("10.42." + (n / 250) + "." + (n % 250 + 1));
                    return solicitud;
                }));
    }

    JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    String email(String nombre) {
        return nombre + "-" + sufijo;
    }

    Long ficha(String email) {
        return jdbc.queryForObject("""
                insert into pacientes (nombre, apellido, email, telefono, activo, creado_en)
                values ('Beatriz', 'Previa', ?, '1155550001', true, now()) returning id
                """, Long.class, email);
    }

    Long turnoEn(String inicio) {
        return jdbc.queryForObject("select id from turno where profesional_id = ? and inicio = ?", Long.class,
                profesional, OffsetDateTime.parse(inicio));
    }

    String token(int indice) {
        var matcher = ENLACE.matcher(correo.enviados().get(indice).cuerpo());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    /** Reserva y, si responde 422, anota la propiedad {@code limite} del Problem Details. */
    int estadoYLimite(ObjectNode pedido, java.util.Collection<String> limites) throws Exception {
        var respuesta = reservar(pedido).andReturn().getResponse();
        if (respuesta.getStatus() == 422) {
            limites.add(json.readTree(respuesta.getContentAsString()).get("limite").asText());
        }
        return respuesta.getStatus();
    }

    /** Ejecuta las dos tareas a la vez y devuelve el estado HTTP de cada una. */
    List<Integer> enParalelo(Callable<Integer> primera, Callable<Integer> segunda) throws Exception {
        var largada = new CountDownLatch(1);
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(() -> {
                largada.await();
                return primera.call();
            });
            var b = hilos.submit(() -> {
                largada.await();
                return segunda.call();
            });
            largada.countDown();
            var estados = new ArrayList<Integer>();
            estados.add(a.get(30, TimeUnit.SECONDS));
            estados.add(b.get(30, TimeUnit.SECONDS));
            return estados;
        }
    }

    // ---------- Hueco ocupado con alternativas ----------

    @Test
    @DisplayName("Dos reservas simultáneas del mismo hueco: una responde 201 y la otra 409")
    void carreraPorElMismoHueco() throws Exception {
        var estados = enParalelo(
                () -> reservar(reserva(LUNES_9, email("uno"))).andReturn().getResponse().getStatus(),
                () -> reservar(reserva(LUNES_9, email("dos"))).andReturn().getResponse().getStatus());

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("select count(*) from turno where profesional_id = ? and inicio = ?",
                Integer.class, profesional, OffsetDateTime.parse(LUNES_9))).isEqualTo(1);
    }

    @Test
    @DisplayName("Un hueco ocupado responde 409 con alternativas")
    void huecoOcupadoConAlternativas() throws Exception {
        reservar(reserva(LUNES_9, email("uno"))).andExpect(status().isCreated());

        var problema = cuerpo(reservar(reserva(LUNES_9, email("dos"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01")));

        assertThat(problema.has("recurso")).isFalse();
        var alternativas = problema.get("alternativas");
        assertThat(alternativas).hasSize(5);
        assertThat(alternativas.get(0).get("inicio").asText()).isEqualTo("2026-12-07T12:30:00Z");
        assertThat(alternativas.get(0).get("fin").asText()).isEqualTo("2026-12-07T13:00:00Z");
        for (var alternativa : alternativas) {
            var campos = new ArrayList<String>();
            alternativa.fieldNames().forEachRemaining(campos::add);
            assertThat(campos).containsExactly("inicio", "fin");
        }
        assertThat(jdbc.queryForObject("select count(*) from pacientes where email = ?", Integer.class,
                email("dos"))).isZero();
    }

    @Test
    @DisplayName("Un 409 sin horarios libres devuelve alternativas vacías")
    void sinAlternativas() throws Exception {
        // Una sola franja de 30 minutos: después de la primera reserva no queda ningún horario libre.
        jdbc.update("delete from horario_atencion where profesional_id = ?", profesional);
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "09:30");
        reservar(reserva(LUNES_9, email("uno"))).andExpect(status().isCreated());

        reservar(reserva(LUNES_9, email("dos"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"))
                .andExpect(jsonPath("$.alternativas").isArray())
                .andExpect(jsonPath("$.alternativas").isEmpty());
    }

    // ---------- Tope por email ----------

    @Test
    @DisplayName("Dos reservas simultáneas del mismo email no superan el tope")
    void carreraPorElTope() throws Exception {
        var compartido = email("tope");
        var existente = ficha(compartido);
        var otroSillon = DatosAgenda.sillon(jdbc, true);
        try {
            DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-08T12:00:00Z",
                    "2026-12-08T12:30:00Z", "PROGRAMADO");
            DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-09T12:00:00Z",
                    "2026-12-09T12:30:00Z", "PROGRAMADO");

            var limites = new java.util.concurrent.ConcurrentLinkedQueue<String>();
            Callable<Integer> primera = () -> estadoYLimite(reserva(LUNES_9, compartido), limites);
            Callable<Integer> segunda = () -> estadoYLimite(reserva("2026-12-07T13:00:00Z", compartido), limites);
            var estados = enParalelo(primera, segunda);

            assertThat(estados).containsExactlyInAnyOrder(201, 422);
            assertThat(limites).containsExactly("TOPE_EMAIL");
            assertThat(jdbc.queryForObject("select count(*) from turno where paciente_id = ?", Integer.class,
                    existente)).isEqualTo(3);
            assertThat(jdbc.queryForObject("select count(*) from pacientes where email = ?", Integer.class,
                    compartido)).isEqualTo(1);
        } finally {
            jdbc.update("delete from turno where sillon_id = ?", otroSillon);
            jdbc.update("delete from sillon where id = ?", otroSillon);
        }
    }

    @Test
    @DisplayName("Dos reservas simultáneas de un email nuevo crean una sola ficha")
    void carreraPorLaFicha() throws Exception {
        var compartido = email("nueva");

        var estados = enParalelo(
                () -> reservar(reserva(LUNES_9, compartido)).andReturn().getResponse().getStatus(),
                () -> reservar(reserva("2026-12-07T13:00:00Z", compartido)).andReturn().getResponse().getStatus());

        assertThat(estados).containsExactly(201, 201);
        assertThat(jdbc.queryForObject("select count(*) from pacientes where email = ?", Integer.class,
                compartido)).isEqualTo(1);
    }

    // ---------- Email de confirmación ----------

    @Test
    @DisplayName("La reserva envía el email de confirmación con el enlace")
    void emailDeConfirmacion() throws Exception {
        var destino = email("correo");
        var token = cuerpo(reservar(reserva(LUNES_9, destino)).andExpect(status().isCreated())).get("token").asText();

        assertThat(correo.enviados()).hasSize(1);
        assertThat(correo.enviados().get(0).destinatario()).isEqualTo(destino);
        assertThat(token(0)).isEqualTo(token);
        var registro = jdbc.queryForMap("select tipo, estado from notificacion_enviada where turno_id = ?",
                turnoEn(LUNES_9));
        assertThat(registro).containsEntry("tipo", "CONFIRMACION_RESERVA").containsEntry("estado", "ENVIADA");
    }

    @Test
    @DisplayName("Un fallo del email deja la reserva hecha y la notificación FALLIDA")
    void emailFallido() throws Exception {
        correo.fallarProximos(1);

        var respuesta = cuerpo(reservar(reserva(LUNES_9, email("falla"))).andExpect(status().isCreated()));

        assertThat(respuesta.get("token").asText()).matches("[A-Za-z0-9_-]{43}");
        var turno = turnoEn(LUNES_9);
        var registro = jdbc.queryForMap("select tipo, estado, intentos, programada_para from notificacion_enviada "
                + "where turno_id = ?", turno);
        assertThat(registro).containsEntry("tipo", "CONFIRMACION_RESERVA").containsEntry("estado", "FALLIDA")
                .containsEntry("intentos", 1);
        assertThat(registro.get("programada_para")).isNotNull();
        assertThat(correo.enviados()).isEmpty();
    }

    @Test
    @DisplayName("El reintento de la confirmación lleva un enlace nuevo válido")
    void reintentoConEnlaceNuevo() throws Exception {
        correo.fallarProximos(1);
        var original = cuerpo(reservar(reserva(LUNES_9, email("reintento"))).andExpect(status().isCreated()))
                .get("token").asText();
        vencerNotificacion(turnoEn(LUNES_9));

        notificaciones.procesarVencidas();

        assertThat(correo.enviados()).hasSize(1);
        var nuevo = token(0);
        assertThat(nuevo).isNotEqualTo(original);
        mockMvc.perform(get("/api/public/turnos/{token}", nuevo)).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/turnos/{token}", original)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select estado from notificacion_enviada where turno_id = ?", String.class,
                turnoEn(LUNES_9))).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("El reintento de un turno cancelado se descarta")
    void reintentoDeTurnoCancelado() throws Exception {
        correo.fallarProximos(1);
        reservar(reserva(LUNES_9, email("cancelado"))).andExpect(status().isCreated());
        var turno = turnoEn(LUNES_9);
        jdbc.update("update turno set estado = 'CANCELADO' where id = ?", turno);
        vencerNotificacion(turno);
        int intentos = correo.intentos();

        notificaciones.procesarVencidas();

        assertThat(correo.intentos()).isEqualTo(intentos);
        assertThat(correo.enviados()).isEmpty();
        var registro = jdbc.queryForMap("select estado, programada_para from notificacion_enviada where turno_id = ?",
                turno);
        assertThat(registro.get("estado")).isEqualTo("FALLIDA");
        assertThat(registro.get("programada_para")).isNull();
    }

    private void vencerNotificacion(Long turno) {
        jdbc.update("update notificacion_enviada set programada_para = ? where turno_id = ?",
                Timestamp.from(RelojFijoConfig.INSTANTE_FIJO.minusSeconds(60)), turno);
    }
}
