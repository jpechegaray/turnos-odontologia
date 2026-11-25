package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios.HorarioDelDia;
import ar.edu.utn.turnos.agenda.domain.ProyeccionHorarios.TramoSemanal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProyeccionHorariosTest {

    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime TRECE = LocalTime.of(13, 0);

    @Test
    @DisplayName("Un tramo de lunes y otro de miércoles se proyectan en sus fechas de la semana")
    void proyectaEnCadaFecha() {
        var tramos = List.of(new TramoSemanal(1L, 1, NUEVE, TRECE), new TramoSemanal(1L, 3, NUEVE, TRECE));

        List<HorarioDelDia> horarios = ProyeccionHorarios.proyectar(tramos,
                PeriodoAgenda.de(VistaAgenda.SEMANA, LocalDate.of(2026, 12, 7)));

        assertThat(horarios).extracting(HorarioDelDia::fecha)
                .containsExactly(LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 9));
        assertThat(horarios.getFirst().inicio()).isEqualTo(Instant.parse("2026-12-07T12:00:00Z"));
        assertThat(horarios.getFirst().fin()).isEqualTo(Instant.parse("2026-12-07T16:00:00Z"));
        assertThat(horarios.getFirst().profesionalId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Un día sin tramos no genera horarios")
    void diaSinTramos() {
        var tramos = List.of(new TramoSemanal(1L, 3, NUEVE, TRECE));

        assertThat(ProyeccionHorarios.proyectar(tramos, PeriodoAgenda.de(VistaAgenda.DIA, LocalDate.of(2026, 12, 7))))
                .isEmpty();
    }

    @Test
    @DisplayName("Los horarios salen ordenados por fecha, profesional e inicio")
    void ordenado() {
        var tramos = List.of(
                new TramoSemanal(2L, 1, NUEVE, TRECE),
                new TramoSemanal(1L, 1, LocalTime.of(15, 0), LocalTime.of(18, 0)),
                new TramoSemanal(1L, 1, NUEVE, TRECE),
                new TramoSemanal(1L, 2, NUEVE, TRECE));

        List<HorarioDelDia> horarios = ProyeccionHorarios.proyectar(tramos,
                PeriodoAgenda.de(VistaAgenda.SEMANA, LocalDate.of(2026, 12, 7)));

        assertThat(horarios).extracting(h -> h.fecha().getDayOfMonth() + "-" + h.profesionalId() + "-"
                + h.inicio()).containsExactly(
                        "7-1-2026-12-07T12:00:00Z",
                        "7-1-2026-12-07T18:00:00Z",
                        "7-2-2026-12-07T12:00:00Z",
                        "8-1-2026-12-08T12:00:00Z");
    }
}
