package ar.edu.utn.turnos.listaespera.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelectorDeAvisosTest {
    private static final Instant INICIO = Instant.parse("2030-06-12T13:00:00Z");
    private static final Hueco HUECO = new Hueco(10L, INICIO, INICIO.plus(Duration.ofMinutes(30)));
    private static final Instant ANTES = INICIO.minus(Duration.ofHours(2));
    private static final Instant BASE = Instant.parse("2030-06-01T12:00:00Z");

    /** Entrada guardada (con id) del paciente, creada {@code minutos} después de la base. */
    private static EntradaListaEspera entrada(long id, long paciente, long minutos) {
        return new EntradaListaEspera(id, paciente, 5L + id, null, LocalDate.of(2030, 6, 1),
                LocalDate.of(2030, 6, 30), null, null, EstadoListaEspera.ACTIVA, null,
                BASE.plus(Duration.ofMinutes(minutos)));
    }

    @Test
    @DisplayName("El tope de avisos elige a los más antiguos")
    void topePorAntiguedad() {
        List<EntradaListaEspera> compatibles = new ArrayList<>(
                LongStream.rangeClosed(1, 12).mapToObj(i -> entrada(i, 100 + i, i)).toList());
        Collections.reverse(compatibles);

        var elegidas = SelectorDeAvisos.seleccionar(compatibles, HUECO, ANTES, 10);

        assertThat(elegidas).extracting(EntradaListaEspera::id)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    @DisplayName("Un paciente con dos entradas compatibles recibe un solo aviso")
    void unAvisoPorPaciente() {
        var nueva = entrada(2L, 100L, 60 * 24 * 3);
        var vieja = entrada(1L, 100L, 0);

        var elegidas = SelectorDeAvisos.seleccionar(List.of(nueva, vieja), HUECO, ANTES, 10);

        assertThat(elegidas).containsExactly(vieja);
    }

    @Test
    @DisplayName("El paciente repetido no ocupa un lugar del tope")
    void repetidoNoOcupaTope() {
        var primeraDeA = entrada(1L, 100L, 0);
        var segundaDeA = entrada(2L, 100L, 1);
        var deB = entrada(3L, 200L, 2);

        var elegidas = SelectorDeAvisos.seleccionar(List.of(deB, segundaDeA, primeraDeA), HUECO, ANTES, 2);

        assertThat(elegidas).containsExactly(primeraDeA, deB);
    }

    @Test
    @DisplayName("No se avisa por un hueco que ya empezó")
    void huecoEmpezado() {
        var compatibles = List.of(entrada(1L, 100L, 0));

        assertThat(SelectorDeAvisos.seleccionar(compatibles, HUECO, INICIO, 10)).isEmpty();
        assertThat(SelectorDeAvisos.seleccionar(compatibles, HUECO, INICIO.plusSeconds(1), 10)).isEmpty();
        assertThat(SelectorDeAvisos.seleccionar(compatibles, HUECO, INICIO.minusMillis(1), 10)).hasSize(1);
    }

    @Test
    @DisplayName("Con la misma antigüedad desempata el id")
    void desempatePorId() {
        var b = entrada(7L, 200L, 0);
        var a = entrada(3L, 100L, 0);

        assertThat(SelectorDeAvisos.seleccionar(List.of(b, a), HUECO, ANTES, 10)).containsExactly(a, b);
    }

    @Test
    @DisplayName("Con tope cero no se avisa a nadie")
    void topeCero() {
        assertThat(SelectorDeAvisos.seleccionar(List.of(entrada(1L, 100L, 0)), HUECO, ANTES, 0)).isEmpty();
    }
}
