package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadorDisponibilidadTest {
    private final CalculadorDisponibilidad calculador = new CalculadorDisponibilidad();
    private static Instant hora(String hora) {
        return Instant.parse("2030-06-10T" + hora + ":00Z");
    }

    @Test
    @DisplayName("Un hueco exacto admite la prestación")
    void huecoExacto() {
        var rango = new Intervalo(hora("12:00"), hora("12:30"));
        var horarios = new CalculadorDisponibilidad().calcular(1L, rango, 30, hora("11:00"),
                List.of(rango), List.of(), List.of(), List.of(2L));
        assertThat(horarios).containsExactly(new HorarioDisponible(hora("12:00"), hora("12:30"), 2L));
    }

    private List<HorarioDisponible> calcular(int duracion, Instant ahora, List<Intervalo> horarios,
            List<Intervalo> bloqueos, List<TurnoOcupado> turnos, List<Long> sillones) {
        return calculador.calcular(1L, new Intervalo(hora("12:00"), hora("13:00")), duracion, ahora,
                horarios, bloqueos, turnos, sillones);
    }

    private List<HorarioDisponible> libre(int duracion) {
        return calcular(duracion, hora("11:00"), List.of(new Intervalo(hora("12:00"), hora("12:30"))),
                List.of(), List.of(), List.of(2L));
    }

    @Test
    @DisplayName("Una prestación mayor al hueco no se ofrece")
    void duracionMayor() {
        assertThat(libre(45)).isEmpty();
        assertThat(libre(30)).hasSize(1);
    }

    @Test
    @DisplayName("Los tramos no se unen para completar una prestación")
    void tramosSeparados() {
        assertThat(calcular(45, hora("11:00"), List.of(
                new Intervalo(hora("12:00"), hora("12:30")),
                new Intervalo(hora("12:30"), hora("13:00"))), List.of(), List.of(), List.of(2L))).isEmpty();
    }

    @Test
    @DisplayName("La búsqueda redondea hacia la próxima grilla")
    void grilla() {
        var rango = new Intervalo(Instant.parse("2030-06-10T12:01:00.001Z"), hora("13:00"));
        var resultado = calculador.calcular(1L, rango, 30, hora("11:00"), List.of(rango), List.of(), List.of(), List.of(2L));
        assertThat(resultado).hasSize(6);
        assertThat(resultado.getFirst().inicio()).isEqualTo(hora("12:05"));
        assertThat(resultado.getLast().inicio()).isEqualTo(hora("12:30"));
    }

    @Test
    @DisplayName("No se ofrecen inicios pasados ni iguales a ahora")
    void futuroEstricto() {
        var tramo = new Intervalo(hora("12:00"), hora("13:00"));
        assertThat(calcular(30, hora("12:15"), List.of(tramo), List.of(), List.of(), List.of(2L)))
                .extracting(HorarioDisponible::inicio).containsExactly(hora("12:20"), hora("12:25"), hora("12:30"));
        assertThat(calcular(30, hora("13:00"), List.of(tramo), List.of(), List.of(), List.of(2L))).isEmpty();
    }

    @Test
    @DisplayName("Una duración inválida se rechaza")
    void duracionInvalida() {
        for (int duracion : new int[] {0, 7, 485}) {
            assertThatThrownBy(() -> libre(duracion)).isInstanceOf(ReglaNegocioException.class)
                    .extracting("codigo").isEqualTo("RN-09");
        }
    }

    private TurnoOcupado turno(Long profesional, Long sillon, String desde, String hasta, EstadoTurno estado) {
        return new TurnoOcupado(10L, profesional, sillon, new Intervalo(hora(desde), hora(hasta)), estado);
    }

    private List<HorarioDisponible> conAgenda(List<Intervalo> bloqueos, List<TurnoOcupado> turnos, List<Long> sillones) {
        return calcular(30, hora("11:00"), List.of(new Intervalo(hora("12:00"), hora("13:00"))), bloqueos, turnos, sillones);
    }

    @Test
    @DisplayName("El borde contiguo de un turno queda disponible")
    void turnoContiguo() {
        assertThat(conAgenda(List.of(), List.of(turno(1L, 99L, "12:00", "12:30", EstadoTurno.PROGRAMADO)), List.of(2L)))
                .extracting(HorarioDisponible::inicio).containsExactly(hora("12:30"));
        assertThat(conAgenda(List.of(), List.of(turno(1L, 99L, "12:30", "13:00", EstadoTurno.CONFIRMADO)), List.of(2L)))
                .extracting(HorarioDisponible::inicio).containsExactly(hora("12:00"));
    }

    @Test
    @DisplayName("Un bloqueo excluye solapamientos y permite el borde")
    void bloqueoContiguo() {
        assertThat(conAgenda(List.of(new Intervalo(hora("12:00"), hora("12:30"))), List.of(), List.of(2L)))
                .extracting(HorarioDisponible::inicio).containsExactly(hora("12:30"));
    }

    @Test
    @DisplayName("Se elige el primer sillón libre para todo el intervalo")
    void sillonAlternativo() {
        var ocupacion = List.of(turno(99L, 2L, "12:25", "12:30", EstadoTurno.PROGRAMADO));
        var resultado = conAgenda(List.of(), ocupacion, List.of(3L, 2L));
        assertThat(resultado).hasSize(7);
        assertThat(resultado.getFirst().sillonId()).isEqualTo(3L);
        assertThat(resultado.getLast().sillonId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Los turnos cancelados no quitan disponibilidad")
    void cancelado() {
        assertThat(conAgenda(List.of(), List.of(turno(1L, 2L, "12:00", "13:00", EstadoTurno.CANCELADO)), List.of(2L)))
                .hasSize(7);
        for (var estado : List.of(EstadoTurno.ATENDIDO, EstadoTurno.AUSENTE)) {
            assertThat(conAgenda(List.of(), List.of(turno(1L, 2L, "12:00", "13:00", estado)), List.of(2L))).isEmpty();
        }
    }

    @Test
    @DisplayName("Sin sillones libres no se ofrecen horarios")
    void sinSillones() {
        assertThat(conAgenda(List.of(), List.of(), List.of())).isEmpty();
        assertThat(conAgenda(List.of(), List.of(turno(99L, 2L, "12:00", "13:00", EstadoTurno.PROGRAMADO)), List.of(2L)))
                .isEmpty();
    }
}
