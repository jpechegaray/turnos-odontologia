package ar.edu.utn.turnos.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.PeriodoAgenda;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.VistaAgenda;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de lectura de la agenda (C-28) con PostgreSQL real. Las horas se escriben en Buenos Aires. */
@Transactional
class AgendaLecturaPersistenciaIT extends PruebaIntegracionBase {
    private static final Intervalo DIA_7 = PeriodoAgenda.de(VistaAgenda.DIA, LocalDate.of(2026, 12, 7)).intervalo();
    private static final Intervalo DIA_8 = PeriodoAgenda.de(VistaAgenda.DIA, LocalDate.of(2026, 12, 8)).intervalo();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AgendaLecturaPersistencia lectura;

    private Long paciente;
    private Long otroPaciente;
    private Long profesional;
    private Long otroProfesional;
    private Long sillon;
    private Long otroSillon;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        otroPaciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        otroSillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    private Long turno(Long prof, Long sil, String inicio, String fin, String estado) {
        return DatosAgenda.turno(jdbc, paciente, prof, sil, prestacion, inicio, fin, estado);
    }

    private static List<Long> ids(List<Turno> turnos) {
        return turnos.stream().map(Turno::id).toList();
    }

    @Test
    @DisplayName("Los bordes de medianoche se resuelven en hora de Buenos Aires")
    void bordesDeMedianoche() {
        Long tarde = turno(profesional, sillon, "2026-12-07T23:30:00-03:00", "2026-12-07T23:55:00-03:00", "PROGRAMADO");
        Long medianoche = turno(profesional, sillon, "2026-12-08T00:00:00-03:00", "2026-12-08T00:30:00-03:00",
                "PROGRAMADO");

        assertThat(ids(lectura.turnosEnPeriodo(DIA_7, profesional, null))).containsExactly(tarde);
        assertThat(ids(lectura.turnosEnPeriodo(DIA_8, profesional, null))).containsExactly(medianoche);
    }

    @Test
    @DisplayName("Un turno que termina justo al empezar el período queda afuera")
    void contiguoQuedaAfuera() {
        turno(profesional, sillon, "2026-12-06T23:30:00-03:00", "2026-12-07T00:00:00-03:00", "PROGRAMADO");

        assertThat(lectura.turnosEnPeriodo(DIA_7, profesional, null)).isEmpty();
    }

    @Test
    @DisplayName("Los turnos se filtran por profesional, por sillón y por ambos, en todos los estados y en orden")
    void filtros() {
        Long cancelado = turno(profesional, sillon, "2026-12-07T09:00:00-03:00", "2026-12-07T09:30:00-03:00",
                "CANCELADO");
        Long propioOtroSillon = turno(profesional, otroSillon, "2026-12-07T10:00:00-03:00",
                "2026-12-07T10:30:00-03:00", "PROGRAMADO");
        Long ajenoMismoSillon = turno(otroProfesional, sillon, "2026-12-07T11:00:00-03:00",
                "2026-12-07T11:30:00-03:00", "CONFIRMADO");

        assertThat(ids(lectura.turnosEnPeriodo(DIA_7, profesional, null))).containsExactly(cancelado, propioOtroSillon);
        assertThat(ids(lectura.turnosEnPeriodo(DIA_7, null, sillon))).containsExactly(cancelado, ajenoMismoSillon);
        assertThat(ids(lectura.turnosEnPeriodo(DIA_7, profesional, sillon))).containsExactly(cancelado);
        assertThat(ids(lectura.turnosEnPeriodo(DIA_7, null, null)))
                .containsSubsequence(cancelado, propioOtroSillon, ajenoMismoSillon);
    }

    @Test
    @DisplayName("Existe turno solo si el paciente tiene uno con ese profesional, aunque esté cancelado")
    void existeTurno() {
        turno(profesional, sillon, "2026-12-07T09:00:00-03:00", "2026-12-07T09:30:00-03:00", "CANCELADO");

        assertThat(lectura.existeTurno(paciente, profesional)).isTrue();
        assertThat(lectura.existeTurno(paciente, otroProfesional)).isFalse();
        assertThat(lectura.existeTurno(otroPaciente, profesional)).isFalse();
    }
}
