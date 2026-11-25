package ar.edu.utn.turnos.reservapublica.gestiontoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import(RelojFijoConfig.class)
class GestionTurnoApiIT extends PruebaIntegracionBase {
    static final String RUTA = "/api/public/turnos/{token}";
    static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;
    static final Instant LIMITE = AHORA.plusSeconds(86400);

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ServicioTokenTurno tokens;
    @MockitoSpyBean(name = "relojFijo")
    Clock reloj;

    @Test
    @DisplayName("El enlace válido devuelve solo el detalle público de su turno")
    void detalle() throws Exception {
        var primero = turno(LIMITE);
        var otro = turno(LIMITE.plusSeconds(1800));
        String token = tokens.emitir(primero.id()).claro();
        assertThat(otro.prestacion()).isNotEqualTo(primero.prestacion());

        mockMvc.perform(get(RUTA, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.inicio").value(LIMITE.toString()))
                .andExpect(jsonPath("$.fin").value(LIMITE.plusSeconds(1800).toString()))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.profesional").value("Luis Ficticio"))
                .andExpect(jsonPath("$.prestacion").value(primero.prestacion()))
                .andExpect(jsonPath("$.cancelacionSeriaTardia").value(false));
    }

    @Test
    @DisplayName("El token del turno A no permite operar sobre el turno B")
    void noOperaOtroTurno() throws Exception {
        var a = turno(LIMITE);
        var b = turno(LIMITE);
        String token = tokens.emitir(a.id()).claro();
        mockMvc.perform(get("/api/public/turnos/{id}/{token}", b.id(), token)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/public/turnos/{id}/{token}/confirmar", b.id(), token))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/public/turnos/{id}/{token}/cancelar", b.id(), token))
                .andExpect(status().isNotFound());
        assertThat(estado(a.id())).isEqualTo("PROGRAMADO");
        assertThat(estado(b.id())).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("La consulta distingue el plazo exacto de la cancelación tardía")
    void plazo() throws Exception {
        assertThat(jdbc.update("update configuracion set valor = ? where clave = ?", "12", "cancelacion.plazo_horas"))
                .isEqualTo(1);
        var inicio = AHORA.plus(Duration.ofHours(12));
        String token = tokens.emitir(turno(inicio).id()).claro();

        mockMvc.perform(get(RUTA, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(inicio.toString()))
                .andExpect(jsonPath("$.cancelacionSeriaTardia").value(false));

        doReturn(AHORA.plusSeconds(1)).when(reloj).instant();
        mockMvc.perform(get(RUTA, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(inicio.toString()))
                .andExpect(jsonPath("$.cancelacionSeriaTardia").value(true));
    }

    @Test
    @DisplayName("No existe reprogramación para el enlace del paciente")
    void noReprograma() throws Exception {
        var turno = turno(LIMITE);
        String token = tokens.emitir(turno.id()).claro();
        var antes = jdbc.queryForMap("select inicio, fin, estado, version from turno where id = ?", turno.id());
        mockMvc.perform(post(RUTA + "/reprogramar", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"" + LIMITE.plusSeconds(3600) + "\"}"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForMap("select inicio, fin, estado, version from turno where id = ?", turno.id()))
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("Las tres operaciones devuelven un 404 indistinguible para credenciales rechazadas")
    void credencialesRechazadas() throws Exception {
        var turno = turno(LIMITE);
        String vencido = tokens.emitir(turno.id()).claro();
        var otro = turno(LIMITE);
        String revocado = tokens.emitir(otro.id()).claro();
        var cripto = new ServicioToken();
        jdbc.update("update turno set inicio = ?, fin = ? where id = ?", Timestamp.from(AHORA),
                Timestamp.from(AHORA.plusSeconds(1800)), turno.id());
        jdbc.update("update token_turno set expira_en = ? where token_hash = ?",
                Timestamp.from(AHORA), cripto.hashear(vencido));
        jdbc.update("update token_turno set revocado_en = ? where token_hash = ?",
                Timestamp.from(AHORA), cripto.hashear(revocado));
        String esperado = null;
        for (var token : List.of("malformado", cripto.generar().claro(), vencido, revocado)) {
            for (var solicitud : solicitudesCon(token)) {
                var cuerpo = mockMvc.perform(solicitud)
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.codigo").value("TOKEN_TURNO_NO_ENCONTRADO"))
                        .andExpect(jsonPath("$.detail").value("Token de turno no encontrado"))
                        .andReturn().getResponse().getContentAsString();
                assertThat(cuerpo).doesNotContain(token);
                if (esperado == null) {
                    esperado = cuerpo;
                }
                assertThat(cuerpo).isEqualTo(esperado);
            }
        }
        assertThat(jdbc.queryForObject(
                "select count(*) from token_turno where turno_id in (?, ?) and ultimo_uso_en is not null",
                Long.class, turno.id(), otro.id())).isZero();
        assertThat(estado(turno.id())).isEqualTo("PROGRAMADO");
        assertThat(estado(otro.id())).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("El paciente confirma su turno sin sesión ni CSRF")
    void confirma() throws Exception {
        var turno = turno(LIMITE);
        String token = tokens.emitir(turno.id()).claro();
        mockMvc.perform(post(RUTA + "/confirmar", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        assertThat(estado(turno.id())).isEqualTo("CONFIRMADO");
        mockMvc.perform(get(RUTA, token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    @DisplayName("Volver a confirmar no modifica el turno confirmado")
    void confirmaDeNuevo() throws Exception {
        var turno = turno(LIMITE);
        String token = tokens.emitir(turno.id()).claro();
        mockMvc.perform(post(RUTA + "/confirmar", token)).andExpect(status().isOk());
        Long version = jdbc.queryForObject("select version from turno where id = ?", Long.class, turno.id());
        mockMvc.perform(post(RUTA + "/confirmar", token)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TURNO_TRANSICION_INVALIDA"));
        assertThat(estado(turno.id())).isEqualTo("CONFIRMADO");
        assertThat(jdbc.queryForObject("select version from turno where id = ?", Long.class, turno.id()))
                .isEqualTo(version);
    }

    @Test
    @DisplayName("El límite por IP cubre lecturas y mutaciones del enlace")
    void limitePorIp() throws Exception {
        var turno = turno(LIMITE);
        String valido = tokens.emitir(turno.id()).claro();
        String inexistente = new ServicioToken().generar().claro();
        for (int i = 0; i < 60; i++) {
            boolean conocido = i < 30;
            mockMvc.perform(get(RUTA, conocido ? valido : inexistente).with(GestionTurnoApiIT::mismaIp))
                    .andExpect(status().is(conocido ? 200 : 404));
        }
        jdbc.update("update token_turno set ultimo_uso_en = null where turno_id = ?", turno.id());
        for (var solicitud : List.of(get(RUTA, valido), post(RUTA + "/confirmar", valido),
                post(RUTA + "/cancelar", valido))) {
            mockMvc.perform(solicitud.with(GestionTurnoApiIT::mismaIp)).andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "60"))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.codigo").value("RN-27"));
        }
        assertThat(estado(turno.id())).isEqualTo("PROGRAMADO");
        assertThat(jdbc.queryForObject("select ultimo_uso_en from token_turno where turno_id = ?",
                Timestamp.class, turno.id())).isNull();
    }

    private static MockHttpServletRequest mismaIp(MockHttpServletRequest solicitud) {
        solicitud.setRemoteAddr("192.0.2.39");
        return solicitud;
    }

    private List<MockHttpServletRequestBuilder> solicitudesCon(String token) {
        return List.of(get(RUTA, token), post(RUTA + "/confirmar", token), post(RUTA + "/cancelar", token));
    }

    String estado(Long id) {
        return jdbc.queryForObject("select estado from turno where id = ?", String.class, id);
    }

    TurnoPrueba turno(Instant inicio) {
        Long paciente = DatosAgenda.paciente(jdbc, true);
        Long profesional = DatosAgenda.profesional(jdbc, true);
        Long sillon = DatosAgenda.sillon(jdbc, true);
        Long prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        Long id = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plusSeconds(1800).toString(), "PROGRAMADO");
        return new TurnoPrueba(id,
                jdbc.queryForObject("select nombre from prestacion where id = ?", String.class, prestacion));
    }

    record TurnoPrueba(Long id, String prestacion) {
    }
}
