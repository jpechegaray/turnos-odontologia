package ar.edu.utn.turnos.listaespera.oferta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.agenda.DatosAgenda;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Aviso automático por el horario que libera una reprogramación (C-51, RN-18, RN-23; C-35 publica
 * {@code HuecoLiberado} por cada tramo liberado). Mismo armado que {@link AvisoHuecoLiberadoIT}: reloj fijo el
 * martes 1/12/2026 09:00 de Buenos Aires y turno el viernes 4/12 a las 09:00. El profesional atiende viernes y
 * lunes de 08:00 a 13:00, para que la reprogramación cumpla el horario de atención (RN-04).
 */
class AvisoHuecoReprogramacionIT extends EscenarioOfertasIT {

    @BeforeEach
    void horarios() {
        DatosAgenda.horario(jdbc, profesional, 5, "08:00", "13:00");
        DatosAgenda.horario(jdbc, profesional, 1, "08:00", "13:00");
    }

    static Instant instante(Object valor) {
        return ((Timestamp) valor).toInstant();
    }

    ResultActions reprogramar(Long turno, Instant inicio) throws Exception {
        return mockMvc.perform(post("/api/turnos/{id}/reprogramar", turno).with(comoRecepcion).with(csrfValido())
                .contentType(MediaType.APPLICATION_JSON).content("{\"inicio\":\"" + inicio + "\"}"));
    }

    @Test
    @DisplayName("Una reprogramación avisa por el horario que dejó libre")
    void unaReprogramacionAvisaPorElHorarioQueDejoLibre() throws Exception {
        var interesado = paciente("reprogramacion@oferta.test");
        var entrada = entrada(interesado, consulta, "2026-11-01T10:00:00Z");
        var turno = turno();

        reprogramar(turno, Instant.parse("2026-12-07T12:00:00Z")).andExpect(status().isOk());

        assertThat(entradasConOferta()).containsExactly(entrada);
        var oferta = ofertas().get(0);
        assertThat(instante(oferta.get("hueco_inicio"))).isEqualTo(INICIO_HUECO);
        assertThat(instante(oferta.get("hueco_fin")))
                .isEqualTo(INICIO_HUECO.plus(Duration.ofMinutes(30)));
        assertThat(destinatarios()).containsExactly(email(interesado));
        assertThat(correo.enviados().get(0).asunto()).startsWith("Hueco disponible");
    }

    @Test
    @DisplayName("Una reprogramación que se superpone avisa solo por el tramo liberado")
    void unaReprogramacionQueSeSuperponeAvisaSoloPorElTramoLiberado() throws Exception {
        var hora = DatosAgenda.prestacion(jdbc, 60, true);
        otrasPrestaciones.add(hora);
        DatosAgenda.asignarPrestacion(jdbc, profesional, hora);
        var interesado = paciente("superpuesta@oferta.test");
        entrada(interesado, consulta, "2026-11-01T10:00:00Z");
        Instant diez = Instant.parse("2026-12-04T13:00:00Z");
        var turno = DatosAgenda.turno(jdbc, pacienteTurno, profesional, sillon, hora, diez.toString(),
                diez.plus(Duration.ofMinutes(60)).toString(), "PROGRAMADO");

        reprogramar(turno, diez.plus(Duration.ofMinutes(30))).andExpect(status().isOk());

        var ofertas = ofertas();
        assertThat(ofertas).hasSize(1);
        assertThat(instante(ofertas.get(0).get("hueco_inicio"))).isEqualTo(diez);
        assertThat(instante(ofertas.get(0).get("hueco_fin")))
                .isEqualTo(diez.plus(Duration.ofMinutes(30)));
    }
}
