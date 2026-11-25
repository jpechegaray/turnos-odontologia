package ar.edu.utn.turnos.listaespera.oferta.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Toma del hueco ofrecido (C-42, specs toma-oferta-hueco; RN-15, RN-24, RN-25, RN-38). */
class TomaOfertaIT extends EscenarioTomaOfertaIT {
    private static final Pattern ENLACE_TURNO =
            Pattern.compile("https://turnos\\.consultorio\\.test/turno/([A-Za-z0-9_-]{43})");

    @Autowired
    ObjectMapper json;

    @Autowired
    ServicioTokenTurno tokensTurno;

    private JsonNode tomarConExito(String token) throws Exception {
        var cuerpo = tomar(token).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    private java.util.Map<String, Object> turnoDe(Long entrada) {
        return jdbc.queryForMap("""
                select t.* from turno t join lista_espera e on e.turno_asignado_id = t.id where e.id = ?
                """, entrada);
    }

    @Test
    @DisplayName("El paciente toma un hueco disponible")
    void tomaDisponible() throws Exception {
        var paciente = paciente("gana@lista.test");
        var entrada = entrada(paciente);
        var token = oferta(entrada, INICIO_HUECO, FIN_HUECO, sillon);

        var respuesta = tomarConExito(token);

        assertThat(respuesta.get("estado").asText()).isEqualTo("PROGRAMADO");
        assertThat(respuesta.get("inicio").asText()).isEqualTo(INICIO_HUECO.toString());
        assertThat(respuesta.get("fin").asText()).isEqualTo(FIN_TURNO.toString());
        assertThat(respuesta.get("profesional").asText()).isEqualTo("Luis Ficticio");
        assertThat(respuesta.get("token").asText()).matches("[A-Za-z0-9_-]{43}");
        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
        assertThat(estadoOferta(token)).isEqualTo("TOMADA");
        var turno = turnoDe(entrada);
        assertThat(turno.get("canal")).isEqualTo("ONLINE");
        assertThat(turno.get("estado")).isEqualTo("PROGRAMADO");
        assertThat(((Number) turno.get("paciente_id")).longValue()).isEqualTo(paciente);
        assertThat(((Number) turno.get("prestacion_id")).longValue()).isEqualTo(consulta);
        assertThat(((Number) turno.get("profesional_id")).longValue()).isEqualTo(profesional);
        assertThat(((Number) turno.get("sillon_id")).longValue()).isEqualTo(sillon);
        assertThat(((Timestamp) turno.get("inicio")).toInstant()).isEqualTo(INICIO_HUECO);
        assertThat(((Timestamp) turno.get("fin")).toInstant()).isEqualTo(FIN_TURNO);
        assertThat(turno.get("creado_por_usuario_id")).isNull();
        // El token de la respuesta es el del turno nuevo: abre su gestión pública (C-39).
        assertThat(tokensTurno.validar(respuesta.get("token").asText())).isEqualTo(((Number) turno.get("id")).longValue());
    }

    @Test
    @DisplayName("Si el sillón sugerido está ocupado se usa el primero activo libre")
    void sillonSugeridoOcupado() throws Exception {
        var segundo = nuevoSillon();
        var tercero = nuevoSillon();
        var ocupante = nuevoProfesional();
        turnoAjeno(ocupante, sillon, INICIO_HUECO, FIN_TURNO);
        var entrada = entrada(paciente("sillon@lista.test"));
        var token = oferta(entrada, INICIO_HUECO, FIN_HUECO, sillon);

        tomarConExito(token);

        assertThat(((Number) turnoDe(entrada).get("sillon_id")).longValue()).isEqualTo(Math.min(segundo, tercero));
    }

    @Test
    @DisplayName("Las demás ofertas del hueco pasan a tomadas")
    void demasOfertasTomadas() throws Exception {
        var tokens = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            tokens.add(ofertaDisponible());
        }

        tomarConExito(tokens.get(0));

        assertThat(tokens).allSatisfy(t -> assertThat(estadoOferta(t)).isEqualTo("TOMADA"));
        consultar(tokens.get(1)).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("TOMADA"));
        consultar(tokens.get(2)).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("TOMADA"));
    }

    @Test
    @DisplayName("Volver a tomar una oferta ya tomada")
    void retomar() throws Exception {
        var token = ofertaDisponible();
        tomarConExito(token);

        tomar(token).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("HUECO_YA_TOMADO"));

        assertThat(turnosDelProfesional()).isEqualTo(1);
    }

    @Test
    @DisplayName("El hueco lo ocupó recepción antes de que el paciente confirme")
    void ocupadoPorRecepcion() throws Exception {
        var token = ofertaDisponible();
        turnoAjeno(profesional, nuevoSillon(), INICIO_HUECO, FIN_TURNO);

        tomar(token).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("HUECO_YA_TOMADO"));

        assertThat(estadoOferta(token)).isEqualTo("TOMADA");
        assertThat(estadoEntrada(entradaDe(token))).isEqualTo("ACTIVA");
        assertThat(turnosDelProfesional()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tomar una oferta cuyo hueco ya empezó")
    void huecoEmpezado() throws Exception {
        var token = ofertaDisponible();
        var empezo = AHORA.minus(Duration.ofMinutes(1));
        jdbc.update("update oferta_hueco set hueco_inicio = ?, expira_en = ? where token_hash = ?",
                Timestamp.from(empezo), Timestamp.from(empezo), tokens.hashear(token));

        tomar(token).andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("OFERTA_VENCIDA"));

        assertThat(turnosDelProfesional()).isZero();
        assertThat(estadoOferta(token)).isEqualTo("VENCIDA");
    }

    @Test
    @DisplayName("Se puede tomar un hueco que empieza en pocos minutos")
    void huecoInminente() throws Exception {
        var inicio = AHORA.plus(Duration.ofMinutes(10));
        var token = oferta(entrada(paciente("pronto@lista.test")), inicio, inicio.plus(Duration.ofMinutes(30)), sillon);

        tomar(token).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("La entrada fue cancelada antes de tomar la oferta")
    void entradaCancelada() throws Exception {
        var token = ofertaDisponible();
        jdbc.update("update lista_espera set estado = 'CANCELADA' where id = ?", entradaDe(token));

        tomar(token).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_DISPONIBLE"));

        assertThat(turnosDelProfesional()).isZero();
        assertThat(estadoEntrada(entradaDe(token))).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("El profesional bloqueó la agenda en el horario del hueco")
    void bloqueoDeAgenda() throws Exception {
        var token = ofertaDisponible();
        DatosAgenda.bloqueo(jdbc, profesional, INICIO_HUECO.toString(), FIN_HUECO.toString());

        tomar(token).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("OFERTA_NO_DISPONIBLE"));

        assertThat(turnosDelProfesional()).isZero();
        assertThat(estadoEntrada(entradaDe(token))).isEqualTo("ACTIVA");
    }

    @Test
    @DisplayName("Tokens inválidos responden el mismo 404")
    void tokensInvalidos() throws Exception {
        var ofertaOtra = ofertaDisponible();
        tomarConExito(ofertaOtra);
        var turno = jdbc.queryForObject("select id from turno where profesional_id = ?", Long.class, profesional);
        var tokenDeTurno = tokensTurno.emitir(turno).claro();
        var invalidos = List.of(tokens.generar().claro(), "no-es-un-token", tokenDeTurno);

        var cuerpos = new ArrayList<String>();
        for (var token : invalidos) {
            cuerpos.add(consultar(token).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"))
                    .andReturn().getResponse().getContentAsString());
            cuerpos.add(tomar(token).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("OFERTA_NO_ENCONTRADA"))
                    .andReturn().getResponse().getContentAsString());
        }

        assertThat(cuerpos).allSatisfy(c -> assertThat(c).isEqualTo(cuerpos.get(0)));
        assertThat(turnosDelProfesional()).isEqualTo(1);
    }

    @Test
    @DisplayName("El paciente recibe el enlace de su nuevo turno")
    void emailConEnlace() throws Exception {
        var paciente = paciente("enlace@lista.test");
        var token = oferta(entrada(paciente), INICIO_HUECO, FIN_HUECO, sillon);
        var email = jdbc.queryForObject("select email from pacientes where id = ?", String.class, paciente);

        tomarConExito(token);

        assertThat(correo.enviados()).hasSize(1);
        MensajeNotificacion mensaje = correo.enviados().get(0);
        assertThat(mensaje.destinatario()).isEqualTo(email);
        var enlace = ENLACE_TURNO.matcher(mensaje.cuerpo());
        assertThat(enlace.find()).as("el email lleva el enlace del turno").isTrue();
        mockMvc.perform(get("/api/public/turnos/{token}", enlace.group(1)).with(EscenarioTomaOfertaIT::ipNueva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(INICIO_HUECO.toString()));
        var registro = jdbc.queryForMap("""
                select n.estado, n.tipo, n.turno_id from notificacion_enviada n where n.paciente_id = ?
                """, paciente);
        assertThat(registro.get("estado")).isEqualTo("ENVIADA");
        assertThat(registro.get("tipo")).isEqualTo("CONFIRMACION_RESERVA");
        assertThat(registro.get("turno_id")).isNotNull();
    }

    @Test
    @DisplayName("Una falla de correo no pierde el turno")
    void fallaDeCorreo() throws Exception {
        var paciente = paciente("falla@lista.test");
        var token = oferta(entrada(paciente), INICIO_HUECO, FIN_HUECO, sillon);
        correo.fallarProximos(1);

        tomar(token).andExpect(status().isCreated());

        assertThat(turnosDelProfesional()).isEqualTo(1);
        var registro = jdbc.queryForMap("""
                select estado, tipo, ultimo_error from notificacion_enviada where paciente_id = ?
                """, paciente);
        assertThat(registro.get("estado")).isEqualTo("FALLIDA");
        assertThat(registro.get("tipo")).isEqualTo("CONFIRMACION_RESERVA");
        assertThat(registro.get("ultimo_error")).isNotNull();
    }
}
