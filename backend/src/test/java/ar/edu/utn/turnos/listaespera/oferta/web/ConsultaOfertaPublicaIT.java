package ar.edu.utn.turnos.listaespera.oferta.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consulta pública de una oferta por su token (C-42, spec toma-oferta-hueco). */
class ConsultaOfertaPublicaIT extends EscenarioTomaOfertaIT {

    @Test
    @DisplayName("Se consulta una oferta disponible")
    void disponible() throws Exception {
        var paciente = paciente("consulta@lista.test");
        var token = oferta(entrada(paciente), INICIO_HUECO, FIN_HUECO, sillon);
        var email = jdbc.queryForObject("select email from pacientes where id = ?", String.class, paciente);

        var cuerpo = consultar(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.inicio").value(INICIO_HUECO.toString()))
                .andExpect(jsonPath("$.fin").value(FIN_TURNO.toString()))
                .andExpect(jsonPath("$.profesional").value("Luis Ficticio"))
                .andExpect(jsonPath("$.prestacion").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain(email, "Ana", "1100000000", token);
        assertThat(estadoOferta(token)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("Una oferta tomada informa que el hueco ya fue tomado")
    void tomada() throws Exception {
        var token = ofertaDisponible();
        jdbc.update("update oferta_hueco set estado = 'TOMADA' where token_hash = ?", tokens.hashear(token));

        consultar(token).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("TOMADA"));
    }

    @Test
    @DisplayName("Una oferta cuyo hueco ya empezó informa que venció")
    void vencida() throws Exception {
        var token = ofertaDisponible();
        var empezo = AHORA.minus(Duration.ofMinutes(1));
        jdbc.update("update oferta_hueco set hueco_inicio = ?, expira_en = ? where token_hash = ?",
                Timestamp.from(empezo), Timestamp.from(empezo), tokens.hashear(token));

        consultar(token).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("VENCIDA"));
        assertThat(estadoOferta(token)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("Una oferta de una entrada que ya no está activa no está disponible")
    void entradaCancelada() throws Exception {
        var token = ofertaDisponible();
        jdbc.update("update lista_espera set estado = 'CANCELADA' where id = ?", entradaDe(token));

        consultar(token).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("NO_DISPONIBLE"));
    }
}
