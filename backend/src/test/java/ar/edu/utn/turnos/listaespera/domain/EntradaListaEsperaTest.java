package ar.edu.utn.turnos.listaespera.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EntradaListaEsperaTest {

    @Test
    @DisplayName("Desde ACTIVA se puede asignar, cancelar o vencer")
    void transicionesDesdeActiva() {
        var activa = nuevaActiva();
        assertThat(activa.estado()).isEqualTo(EstadoListaEspera.ACTIVA);

        var asignada = activa.asignar(77L);
        assertThat(asignada.estado()).isEqualTo(EstadoListaEspera.ASIGNADA);
        assertThat(asignada.turnoAsignadoId()).isEqualTo(77L);

        assertThat(activa.cancelar().estado()).isEqualTo(EstadoListaEspera.CANCELADA);
        assertThat(activa.vencer().estado()).isEqualTo(EstadoListaEspera.VENCIDA);
        // Las transiciones no mutan la entrada original.
        assertThat(activa.estado()).isEqualTo(EstadoListaEspera.ACTIVA);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoListaEspera.class, names = {"ASIGNADA", "CANCELADA", "VENCIDA"})
    @DisplayName("Un estado final no admite otra transición")
    void estadoFinalNoAdmiteTransicion(EstadoListaEspera estadoFinal) {
        var entrada = llevarA(estadoFinal);
        for (UnaryOperator<EntradaListaEspera> transicion : java.util.List.<UnaryOperator<EntradaListaEspera>>of(
                e -> e.asignar(1L), EntradaListaEspera::cancelar, EntradaListaEspera::vencer)) {
            assertThatThrownBy(() -> transicion.apply(entrada))
                    .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                        assertThat(error.codigo()).isEqualTo("ENTRADA_NO_ACTIVA");
                        assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    });
        }
        assertThat(entrada.estado()).isEqualTo(estadoFinal);
    }

    @Test
    @DisplayName("Asignar sin turno se rechaza")
    void asignarSinTurno() {
        assertThatThrownBy(() -> nuevaActiva().asignar(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Una entrada nueva exige fin no anterior al inicio y franja completa y ordenada")
    void invariantesDeLaEntradaNueva() {
        var desde = LocalDate.of(2026, 12, 1);
        assertThatThrownBy(() -> EntradaListaEspera.nueva(1L, 2L, null, desde, desde.minusDays(1), null, null,
                Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntradaListaEspera.nueva(1L, 2L, null, desde, desde, java.time.LocalTime.NOON, null,
                Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EntradaListaEspera.nueva(1L, 2L, null, desde, desde, java.time.LocalTime.NOON,
                java.time.LocalTime.NOON, Instant.EPOCH)).isInstanceOf(IllegalArgumentException.class);
        assertThat(EntradaListaEspera.nueva(1L, 2L, null, desde, desde, null, null, Instant.EPOCH).estado())
                .isEqualTo(EstadoListaEspera.ACTIVA);
    }

    private static EntradaListaEspera nuevaActiva() {
        return EntradaListaEspera.nueva(1L, 2L, 3L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31),
                null, null, Instant.parse("2026-12-01T12:00:00Z"));
    }

    private static EntradaListaEspera llevarA(EstadoListaEspera estado) {
        var activa = nuevaActiva();
        return switch (estado) {
            case ASIGNADA -> activa.asignar(9L);
            case CANCELADA -> activa.cancelar();
            case VENCIDA -> activa.vencer();
            case ACTIVA -> activa;
        };
    }
}
