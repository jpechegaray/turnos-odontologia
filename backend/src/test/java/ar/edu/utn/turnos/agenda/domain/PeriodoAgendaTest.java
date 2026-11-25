package ar.edu.utn.turnos.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PeriodoAgendaTest {

    @Test
    @DisplayName("El día abarca de 00:00 a 24:00 en hora de Buenos Aires")
    void diaEnHoraLocal() {
        PeriodoAgenda periodo = PeriodoAgenda.de(VistaAgenda.DIA, LocalDate.of(2026, 12, 7));

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(periodo.hasta()).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(periodo.intervalo().inicio()).isEqualTo(Instant.parse("2026-12-07T03:00:00Z"));
        assertThat(periodo.intervalo().fin()).isEqualTo(Instant.parse("2026-12-08T03:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-12-07", "2026-12-09", "2026-12-13"})
    @DisplayName("La semana va de lunes a domingo para cualquier día de la semana")
    void semanaDeLunesADomingo(String fecha) {
        PeriodoAgenda periodo = PeriodoAgenda.de(VistaAgenda.SEMANA, LocalDate.parse(fecha));

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(periodo.hasta()).isEqualTo(LocalDate.of(2026, 12, 13));
        assertThat(periodo.intervalo().inicio()).isEqualTo(Instant.parse("2026-12-07T03:00:00Z"));
        assertThat(periodo.intervalo().fin()).isEqualTo(Instant.parse("2026-12-14T03:00:00Z"));
    }

    @Test
    @DisplayName("Las fechas del período van de desde a hasta")
    void fechasDelPeriodo() {
        assertThat(PeriodoAgenda.de(VistaAgenda.SEMANA, LocalDate.of(2026, 12, 10)).fechas())
                .hasSize(7)
                .startsWith(LocalDate.of(2026, 12, 7))
                .endsWith(LocalDate.of(2026, 12, 13));
    }

    @Test
    @DisplayName("La vista se lee sin distinguir mayúsculas y rechaza valores desconocidos")
    void vistaDesdeTexto() {
        assertThat(VistaAgenda.desdeTexto("dia")).isEqualTo(VistaAgenda.DIA);
        assertThat(VistaAgenda.desdeTexto("SEMANA")).isEqualTo(VistaAgenda.SEMANA);
        assertThatThrownBy(() -> VistaAgenda.desdeTexto("mes")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VistaAgenda.desdeTexto(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
