package ar.edu.utn.turnos.reservapublica.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva online (C-41) contra PostgreSQL real, con rollback al final de cada prueba. Reloj fijo: martes
 * 1/12/2026 09:00 de Buenos Aires (12:00Z): la anticipación mínima por defecto llega a las 14:00Z de ese día y el
 * horizonte al 30/01/2027 12:00Z. El profesional atiende los lunes de 09:00 a 11:00 (12:00Z a 14:00Z).
 *
 * <p>Los 409 con alternativas, las carreras y el email se prueban con commit en {@code ReservaOnlineConcurrenciaIT}.
 */
@Transactional
@Import(RelojFijoConfig.class)
class ReservaOnlineApiIT extends PruebaIntegracionBase {

    static final String RESERVAS = "/api/public/reservas";
    static final String LUNES_9 = "2026-12-07T12:00:00Z";
    static final String VERSION = "2026-11-v1";
    private static final AtomicInteger IPS = new AtomicInteger();

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    Long profesional;
    Long sillon;
    Long prestacion;
    String email;

    @BeforeEach
    void preparar() {
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "11:00");
        email = "ana-" + UUID.randomUUID().toString().substring(0, 8) + "@reserva-c41.test";
    }

    // ---------- Ayudantes ----------

    ObjectNode reserva(String inicio) {
        var cuerpo = json.createObjectNode();
        cuerpo.put("prestacionId", prestacion);
        cuerpo.put("profesionalId", profesional);
        cuerpo.put("inicio", inicio);
        cuerpo.put("nombre", "Ana");
        cuerpo.put("apellido", "Ficticia");
        cuerpo.put("email", email);
        cuerpo.put("telefono", "11 5555-0001");
        cuerpo.put("obraSocial", "Obra Social Ficticia");
        cuerpo.put("aceptaTratamientoDatos", true);
        cuerpo.put("versionConsentimiento", VERSION);
        return cuerpo;
    }

    ObjectNode reserva(String inicio, Consumer<ObjectNode> ajuste) {
        var cuerpo = reserva(inicio);
        ajuste.accept(cuerpo);
        return cuerpo;
    }

    ResultActions pedir(MockHttpServletRequestBuilder pedido) throws Exception {
        int n = IPS.incrementAndGet();
        return mockMvc.perform(pedido.with(solicitud -> {
            solicitud.setRemoteAddr("10.41." + (n / 250) + "." + (n % 250 + 1));
            return solicitud;
        }));
    }

    ResultActions reservar(ObjectNode cuerpo) throws Exception {
        return pedir(post(RESERVAS).contentType(MediaType.APPLICATION_JSON).content(cuerpo.toString()));
    }

    ResultActions reservarTexto(String cuerpo) throws Exception {
        return pedir(post(RESERVAS).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    Long ficha(String emailFicha, String telefono, boolean activa) {
        return jdbc.queryForObject("""
                insert into pacientes (nombre, apellido, email, telefono, obra_social, activo, creado_en)
                values ('Beatriz', 'Previa', ?, ?, 'Otra Obra Social', ?, now()) returning id
                """, Long.class, emailFicha, telefono, activa);
    }

    int turnosDelProfesional() {
        return jdbc.queryForObject("select count(*) from turno where profesional_id = ?", Integer.class, profesional);
    }

    int fichasConEmail(String buscado) {
        return jdbc.queryForObject("select count(*) from pacientes where lower(email) = lower(?)", Integer.class,
                buscado);
    }

    Long pacienteDelTurno(String inicio) {
        return jdbc.queryForObject("select paciente_id from turno where profesional_id = ? and inicio = ?",
                Long.class, profesional, OffsetDateTime.parse(inicio));
    }

    void sinTurnoNiFicha() {
        assertThat(turnosDelProfesional()).isZero();
        assertThat(fichasConEmail(email)).isZero();
    }

    // ---------- Datos de la reserva online ----------

    @Test
    @DisplayName("Una reserva válida responde 201 con el turno y el token")
    void reservaValida() throws Exception {
        var respuesta = cuerpo(reservar(reserva(LUNES_9)).andExpect(status().isCreated()));

        assertThat(respuesta.get("inicio").asText()).isEqualTo(LUNES_9);
        assertThat(respuesta.get("fin").asText()).isEqualTo("2026-12-07T12:30:00Z");
        assertThat(respuesta.get("prestacion").asText()).startsWith("Prestación ");
        assertThat(respuesta.get("profesional").asText()).isEqualTo("Luis Ficticio");
        assertThat(respuesta.get("token").asText()).matches("[A-Za-z0-9_-]{43}");
        var turno = jdbc.queryForMap("select estado, canal, duracion_min, creado_por_usuario_id from turno "
                + "where profesional_id = ?", profesional);
        assertThat(turno.get("estado")).isEqualTo("PROGRAMADO");
        assertThat(turno.get("canal")).isEqualTo("ONLINE");
        assertThat(turno.get("duracion_min")).isEqualTo(30);
        assertThat(turno.get("creado_por_usuario_id")).isNull();
    }

    @Test
    @DisplayName("Una reserva sin email o sin obra social responde 400")
    void datosObligatorios() throws Exception {
        List<ObjectNode> invalidos = new ArrayList<>();
        invalidos.add(reserva(LUNES_9, c -> c.remove("email")));
        invalidos.add(reserva(LUNES_9, c -> c.remove("telefono")));
        invalidos.add(reserva(LUNES_9, c -> c.remove("obraSocial")));
        invalidos.add(reserva(LUNES_9, c -> c.put("nombre", "  ")));
        invalidos.add(reserva(LUNES_9, c -> c.put("email", "no-es-un-email")));
        invalidos.add(reserva(LUNES_9, c -> c.remove("inicio")));
        for (var invalido : invalidos) {
            reservar(invalido).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores").isArray());
        }
        reservarTexto("{").andExpect(status().isBadRequest());
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("La reserva no pide ni guarda DNI")
    void sinDni() throws Exception {
        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("select dni from pacientes where email = ?", String.class, email)).isNull();
    }

    // ---------- Consentimiento ----------

    @Test
    @DisplayName("Reservar sin aceptar el tratamiento de datos responde 422")
    void sinAceptacion() throws Exception {
        reservar(reserva(LUNES_9, c -> c.put("aceptaTratamientoDatos", false)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RN-26"));
        reservar(reserva(LUNES_9, c -> c.remove("aceptaTratamientoDatos")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RN-26"));
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("Reservar con una versión de consentimiento que no es la vigente responde 422")
    void versionNoVigente() throws Exception {
        for (var version : new String[] {"2025-01-v0", "", null}) {
            reservar(reserva(LUNES_9, c -> c.put("versionConsentimiento", version)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.codigo").value("CONSENTIMIENTO_VERSION_NO_VIGENTE"));
        }
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("Reservar con una ficha nueva guarda el consentimiento en la ficha")
    void consentimientoEnFichaNueva() throws Exception {
        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        var ficha = jdbc.queryForMap("select consentimiento_datos_en, consentimiento_version from pacientes "
                + "where email = ?", email);
        assertThat(((java.sql.Timestamp) ficha.get("consentimiento_datos_en")).toInstant())
                .isEqualTo(RelojFijoConfig.INSTANTE_FIJO);
        assertThat(ficha.get("consentimiento_version")).isEqualTo(VERSION);
        assertThat(jdbc.queryForObject("select count(*) from consentimiento_turno where turno_id in "
                + "(select id from turno where profesional_id = ?)", Integer.class, profesional)).isZero();
    }

    @Test
    @DisplayName("Reservar con una ficha existente guarda el consentimiento en el turno")
    void consentimientoEnElTurno() throws Exception {
        var existente = ficha(email, "1155550009", true);

        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        var turno = jdbc.queryForObject("select id from turno where profesional_id = ?", Long.class, profesional);
        var registro = jdbc.queryForMap("select aceptado_en, version from consentimiento_turno where turno_id = ?",
                turno);
        assertThat(((java.sql.Timestamp) registro.get("aceptado_en")).toInstant())
                .isEqualTo(RelojFijoConfig.INSTANTE_FIJO);
        assertThat(registro.get("version")).isEqualTo(VERSION);
        var ficha = jdbc.queryForMap("select consentimiento_datos_en, consentimiento_version from pacientes "
                + "where id = ?", existente);
        assertThat(ficha.get("consentimiento_datos_en")).isNull();
        assertThat(ficha.get("consentimiento_version")).isNull();
    }

    // ---------- Deduplicación ----------

    @Test
    @DisplayName("Una reserva con una ficha existente no modifica la ficha")
    void fichaExistenteIntacta() throws Exception {
        var existente = ficha(email, "1155550009", true);
        var antes = jdbc.queryForMap("select * from pacientes where id = ?", existente);

        reservar(reserva(LUNES_9, c -> {
            c.put("email", "  " + email.toUpperCase() + " ");
            c.put("nombre", "Otro Nombre");
            c.put("telefono", "1144440000");
            c.put("obraSocial", "Obra Distinta");
        })).andExpect(status().isCreated());

        assertThat(pacienteDelTurno(LUNES_9)).isEqualTo(existente);
        assertThat(fichasConEmail(email)).isEqualTo(1);
        assertThat(jdbc.queryForMap("select * from pacientes where id = ?", existente)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Con varias fichas del mismo email se elige la que coincide en teléfono")
    void variasFichasPorTelefono() throws Exception {
        ficha(email, "1155550001", true);
        var segunda = ficha(email, "1155550002", true);

        reservar(reserva(LUNES_9, c -> c.put("telefono", "11 5555-0002"))).andExpect(status().isCreated());

        assertThat(pacienteDelTurno(LUNES_9)).isEqualTo(segunda);
        assertThat(fichasConEmail(email)).isEqualTo(2);
    }

    @Test
    @DisplayName("Con varias fichas del mismo email y ningún teléfono coincidente se elige la más antigua")
    void variasFichasLaMasAntigua() throws Exception {
        var primera = ficha(email, null, true);
        ficha(email, "1155550002", true);

        reservar(reserva(LUNES_9, c -> c.put("telefono", "1199990000"))).andExpect(status().isCreated());

        assertThat(pacienteDelTurno(LUNES_9)).isEqualTo(primera);
        assertThat(fichasConEmail(email)).isEqualTo(2);
    }

    @Test
    @DisplayName("Un email nuevo crea una ficha con los datos de la reserva")
    void emailNuevoCreaFicha() throws Exception {
        var baja = ficha(email, "1155550001", false);

        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        var nueva = pacienteDelTurno(LUNES_9);
        assertThat(nueva).isNotEqualTo(baja);
        var ficha = jdbc.queryForMap("select nombre, apellido, email, telefono, obra_social, activo from pacientes "
                + "where id = ?", nueva);
        assertThat(ficha).containsEntry("nombre", "Ana").containsEntry("apellido", "Ficticia")
                .containsEntry("email", email).containsEntry("telefono", "11 5555-0001")
                .containsEntry("obra_social", "Obra Social Ficticia").containsEntry("activo", true);
    }

    @Test
    @DisplayName("La respuesta no revela si la ficha ya existía")
    void respuestaUniforme() throws Exception {
        ficha(email, "1155550001", true);
        var conFicha = cuerpo(reservar(reserva(LUNES_9)).andExpect(status().isCreated()));
        var otroEmail = "nuevo-" + email;
        var sinFicha = cuerpo(reservar(reserva("2026-12-07T13:00:00Z", c -> c.put("email", otroEmail)))
                .andExpect(status().isCreated()));

        var campos = List.of("fin", "inicio", "prestacion", "profesional", "token");
        assertThat(nombres(conFicha)).isEqualTo(campos);
        assertThat(nombres(sinFicha)).isEqualTo(campos);
    }

    private static List<String> nombres(JsonNode nodo) {
        var lista = new ArrayList<String>();
        nodo.fieldNames().forEachRemaining(lista::add);
        return lista.stream().sorted().toList();
    }

    // ---------- Límites ----------

    @Test
    @DisplayName("Se supera el tope de turnos activos por email")
    void topePorEmail() throws Exception {
        var existente = ficha(email, null, true);
        var otra = ficha(email, "1155550003", true);
        var otroSillon = DatosAgenda.sillon(jdbc, true);
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-08T12:00:00Z",
                "2026-12-08T12:30:00Z", "PROGRAMADO");
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-09T12:00:00Z",
                "2026-12-09T12:30:00Z", "CONFIRMADO");
        DatosAgenda.turno(jdbc, otra, profesional, otroSillon, prestacion, "2026-12-10T12:00:00Z",
                "2026-12-10T12:30:00Z", "PROGRAMADO");

        reservar(reserva(LUNES_9)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-27"))
                .andExpect(jsonPath("$.limite").value("TOPE_EMAIL"));
        assertThat(turnosDelProfesional()).isEqualTo(3);
    }

    @Test
    @DisplayName("Los turnos cancelados o pasados no cuentan para el tope")
    void canceladosYPasadosNoCuentan() throws Exception {
        var existente = ficha(email, null, true);
        var otroSillon = DatosAgenda.sillon(jdbc, true);
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-08T12:00:00Z",
                "2026-12-08T12:30:00Z", "PROGRAMADO");
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-09T12:00:00Z",
                "2026-12-09T12:30:00Z", "CONFIRMADO");
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-12-10T12:00:00Z",
                "2026-12-10T12:30:00Z", "CANCELADO");
        DatosAgenda.turno(jdbc, existente, profesional, otroSillon, prestacion, "2026-11-30T12:00:00Z",
                "2026-11-30T12:30:00Z", "ATENDIDO");

        reservar(reserva(LUNES_9)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Un inicio antes de la anticipación mínima responde 422")
    void antesDeLaAnticipacion() throws Exception {
        reservar(reserva("2026-12-01T13:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-27"))
                .andExpect(jsonPath("$.limite").value("ANTICIPACION"));
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("Un inicio después del horizonte responde 422")
    void despuesDelHorizonte() throws Exception {
        reservar(reserva("2027-01-31T12:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-27"))
                .andExpect(jsonPath("$.limite").value("HORIZONTE"));
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("Un horario ofrecido en el borde del horizonte se puede reservar")
    void bordeDelHorizonte() throws Exception {
        // Horizonte de 6 días: el borde es el lunes 7/12 a las 09:00 de Buenos Aires (12:00Z).
        jdbc.update("update configuracion set valor = '6' where clave = 'reserva_online.horizonte_dias'");
        DatosAgenda.horario(jdbc, profesional, 1, "08:00", "09:00");
        var ofrecidos = new ArrayList<String>();
        cuerpo(pedir(get("/api/public/disponibilidad").param("profesionalId", profesional.toString())
                .param("prestacionId", prestacion.toString()).param("desde", "2026-12-07T08:00:00-03:00")
                .param("hasta", "2026-12-07T11:00:00-03:00")).andExpect(status().isOk()))
                .forEach(horario -> ofrecidos.add(horario.get("inicio").asText()));
        assertThat(ofrecidos).last().isEqualTo("2026-12-07T11:30:00Z");

        reservar(reserva("2026-12-07T11:30:00Z")).andExpect(status().isCreated());
        reservar(reserva(LUNES_9, c -> c.put("email", "otro-" + email)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.limite").value("HORIZONTE"));
    }

    // ---------- Recursos y reglas de la agenda ----------

    @Test
    @DisplayName("Reservar con un profesional inactivo responde el 404 uniforme")
    void recursosNoDisponibles() throws Exception {
        var inactivo = DatosAgenda.profesional(jdbc, false);
        DatosAgenda.asignarPrestacion(jdbc, inactivo, prestacion);
        var noAsignada = DatosAgenda.prestacion(jdbc, 30, true);
        List<ObjectNode> pedidos = List.of(
                reserva(LUNES_9, c -> c.put("profesionalId", inactivo)),
                reserva(LUNES_9, c -> c.put("prestacionId", Long.MAX_VALUE)),
                reserva(LUNES_9, c -> c.put("prestacionId", noAsignada)));
        for (var pedido : pedidos) {
            reservar(pedido).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("RECURSO_NO_DISPONIBLE"));
        }
        sinTurnoNiFicha();
    }

    @Test
    @DisplayName("El sistema asigna el primer sillón activo libre")
    void primerSillonLibre() throws Exception {
        var segundo = DatosAgenda.sillon(jdbc, true);
        var otroProfesional = DatosAgenda.profesional(jdbc, true);
        var otroPaciente = DatosAgenda.paciente(jdbc, true);
        DatosAgenda.turno(jdbc, otroPaciente, otroProfesional, sillon, prestacion, LUNES_9, "2026-12-07T12:30:00Z",
                "PROGRAMADO");

        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        var turno = jdbc.queryForMap("select sillon_id, duracion_min from turno where profesional_id = ?",
                profesional);
        assertThat(turno.get("sillon_id")).isEqualTo(segundo);
        assertThat(turno.get("duracion_min")).isEqualTo(30);
    }

    @Test
    @DisplayName("Un horario fuera de la atención del profesional responde 422")
    void fueraDeHorario() throws Exception {
        reservar(reserva("2026-12-07T15:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-04"))
                .andExpect(jsonPath("$.motivo").value("FUERA_DE_HORARIO"));
        reservar(reserva("2026-12-07T12:03:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-09"));
        DatosAgenda.bloqueo(jdbc, profesional, "2026-12-07T13:00:00Z", "2026-12-07T14:00:00Z");
        reservar(reserva("2026-12-07T13:00:00Z")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-04"))
                .andExpect(jsonPath("$.motivo").value("BLOQUEO"));
        // La ficha creada se revierte con el turno; acá la transacción de la prueba solo queda marcada para
        // rollback, así que se verifica el turno (la reversión de la ficha la cubre ReservaOnlineConcurrenciaIT).
        assertThat(turnosDelProfesional()).isZero();
    }

    // ---------- Token ----------

    @Test
    @DisplayName("El token de la respuesta da acceso al turno reservado")
    void tokenDaAcceso() throws Exception {
        var token = cuerpo(reservar(reserva(LUNES_9)).andExpect(status().isCreated())).get("token").asText();

        var turno = cuerpo(pedir(get("/api/public/turnos/{token}", token)).andExpect(status().isOk()));
        assertThat(turno.toString()).contains(LUNES_9);
        assertThat(jdbc.queryForObject("select count(*) from token_turno where token_hash = ?", Integer.class,
                token)).isZero();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("El token de la reserva no aparece en los logs")
    void tokenFueraDeLosLogs(CapturedOutput salida) throws Exception {
        var token = cuerpo(reservar(reserva(LUNES_9)).andExpect(status().isCreated())).get("token").asText();

        assertThat(salida.getAll()).doesNotContain(token).doesNotContain(email);
    }

    // ---------- Datos públicos del consultorio ----------

    @Test
    @DisplayName("El consultorio expone los datos del aviso de consentimiento")
    void datosDelConsultorio() throws Exception {
        var datos = cuerpo(pedir(get("/api/public/consultorio")).andExpect(status().isOk()));

        assertThat(nombres(datos)).containsExactly("domicilio", "emailContacto", "nombre", "retencionAnios",
                "versionConsentimiento");
        assertThat(datos.get("nombre").asText()).isEqualTo("Consultorio Ficticio");
        assertThat(datos.get("domicilio").asText()).isEqualTo("Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires");
        assertThat(datos.get("emailContacto").asText()).isEqualTo("contacto@consultorio-ficticio.example");
        assertThat(datos.get("retencionAnios").asInt()).isEqualTo(2);
        assertThat(datos.get("versionConsentimiento").asText()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Los datos del consultorio reflejan la configuración vigente")
    void datosVigentes() throws Exception {
        jdbc.update("update configuracion set valor = 'Calle Inventada 99' where clave = 'consultorio.domicilio'");

        pedir(get("/api/public/consultorio")).andExpect(status().isOk())
                .andExpect(jsonPath("$.domicilio").value("Calle Inventada 99"));
    }

    @Test
    @DisplayName("El turno reservado queda registrado con el instante del reloj")
    void instanteDeCreacion() throws Exception {
        reservar(reserva(LUNES_9)).andExpect(status().isCreated());

        Instant creado = jdbc.queryForObject("select creado_en from turno where profesional_id = ?",
                java.sql.Timestamp.class, profesional).toInstant();
        assertThat(creado).isEqualTo(RelojFijoConfig.INSTANTE_FIJO);
    }
}
