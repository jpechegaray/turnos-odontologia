package ar.edu.utn.turnos.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta.TurnoEnConflicto;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verificación con PostgreSQL real del puerto {@link TurnosActivosConsulta} (RN-17, RN-37, RN-35). */
@Transactional
@Import(RelojFijoConfig.class)
class TurnosActivosPersistenciaIT extends PruebaIntegracionBase {

    private static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TurnosActivosConsulta consulta;

    private Long paciente;
    private Long profesional;
    private Long otroProfesional;
    private Long tercerProfesional;
    private Long sillon;
    private Long otroSillon;
    private Long tercerSillon;
    private Long prestacion;
    private Long otraPrestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        tercerProfesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        otroSillon = DatosAgenda.sillon(jdbc, true);
        tercerSillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        otraPrestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    @Test
    @DisplayName("La consulta por intervalo incluye solo turnos programados o confirmados que se solapan")
    void consultaPorIntervalo() {
        // Intervalo consultado: 2030-06-10 10:00..12:00 hora Buenos Aires (13:00..15:00 UTC)
        Instant inicio = Instant.parse("2030-06-10T13:00:00Z");
        Instant fin = Instant.parse("2030-06-10T15:00:00Z");

        // Se inserta primero el de las 14:00 (id menor) y luego el de las 13:00 (id mayor) para verificar inicio ASC.
        Long confirmadoTarde = turno(profesional, sillon, prestacion, "2030-06-10T14:00:00Z", "2030-06-10T14:30:00Z",
                "CONFIRMADO");
        Long programadoTemprano = turno(profesional, sillon, prestacion, "2030-06-10T13:00:00Z",
                "2030-06-10T13:30:00Z", "PROGRAMADO");

        // Contiguos (no solapan según RN-02)
        turno(profesional, sillon, prestacion, "2030-06-10T12:30:00Z", "2030-06-10T13:00:00Z", "PROGRAMADO");
        turno(profesional, sillon, prestacion, "2030-06-10T15:00:00Z", "2030-06-10T15:30:00Z", "CONFIRMADO");

        // Estados no activos dentro del intervalo
        turno(profesional, sillon, prestacion, "2030-06-10T13:30:00Z", "2030-06-10T13:45:00Z", "ATENDIDO");
        turno(profesional, sillon, prestacion, "2030-06-10T13:45:00Z", "2030-06-10T14:00:00Z", "AUSENTE");
        turno(profesional, sillon, prestacion, "2030-06-10T14:00:00Z", "2030-06-10T14:30:00Z", "CANCELADO");

        // Otro profesional dentro del mismo intervalo
        turno(otroProfesional, otroSillon, prestacion, "2030-06-10T13:00:00Z", "2030-06-10T13:30:00Z", "PROGRAMADO");

        var resultado = consulta.enIntervaloDeProfesional(profesional, new Intervalo(inicio, fin));

        assertThat(resultado).containsExactly(
                new TurnoEnConflicto(programadoTemprano, Instant.parse("2030-06-10T13:00:00Z"),
                        Instant.parse("2030-06-10T13:30:00Z"), "PROGRAMADO"),
                new TurnoEnConflicto(confirmadoTarde, Instant.parse("2030-06-10T14:00:00Z"),
                        Instant.parse("2030-06-10T14:30:00Z"), "CONFIRMADO"));
    }

    @Test
    @DisplayName("La consulta de turnos futuros activos filtra por fin posterior al reloj actual")
    void consultaFuturosActivos() {
        // Borde exacto fin == AHORA y turno ya finalizado: ambos deben quedar excluidos (fin > ahora)
        turno(profesional, sillon, otraPrestacion, AHORA.minus(Duration.ofMinutes(60)),
                AHORA.minus(Duration.ofMinutes(30)), "PROGRAMADO");
        turno(profesional, sillon, otraPrestacion, AHORA.minus(Duration.ofMinutes(30)), AHORA, "CONFIRMADO");

        // Estados no activos en el futuro: también excluidos
        turno(profesional, sillon, otraPrestacion, AHORA.plus(Duration.ofMinutes(30)), AHORA.plus(Duration.ofMinutes(60)),
                "CANCELADO");
        turno(profesional, sillon, otraPrestacion, AHORA.plus(Duration.ofMinutes(60)), AHORA.plus(Duration.ofMinutes(90)),
                "ATENDIDO");
        turno(profesional, sillon, otraPrestacion, AHORA.plus(Duration.ofMinutes(90)),
                AHORA.plus(Duration.ofMinutes(120)), "AUSENTE");

        // Turnos activos con fin > AHORA:
        // 1) futuroExacto (inicio == AHORA, fin > AHORA)
        Long futuroExacto = turno(profesional, sillon, otraPrestacion, AHORA, AHORA.plus(Duration.ofMinutes(30)),
                "PROGRAMADO");
        // 2) enCurso (inicio < AHORA < fin) en otro profesional/sillón
        Long enCurso = turno(otroProfesional, otroSillon, otraPrestacion, AHORA.minus(Duration.ofMinutes(15)),
                AHORA.plus(Duration.ofMinutes(15)), "CONFIRMADO");
        // 3) empate de inicio con futuroExacto (id mayor) para verificar ordenamiento inicio ASC, id ASC
        Long futuroEmpate = turno(tercerProfesional, tercerSillon, otraPrestacion, AHORA,
                AHORA.plus(Duration.ofMinutes(30)), "CONFIRMADO");
        // 4) turno futuro en el mismo sillón pero con otro profesional y otra prestación
        Long futuroSillon = turno(otroProfesional, sillon, prestacion, AHORA.plus(Duration.ofMinutes(30)),
                AHORA.plus(Duration.ofMinutes(60)), "PROGRAMADO");

        assertThat(consulta.futurosDeProfesional(profesional))
                .extracting(TurnoEnConflicto::id)
                .containsExactly(futuroExacto);

        assertThat(consulta.futurosDeSillon(sillon))
                .extracting(TurnoEnConflicto::id)
                .containsExactly(futuroExacto, futuroSillon);

        assertThat(consulta.futurosDePrestacion(otraPrestacion))
                .extracting(TurnoEnConflicto::id)
                .containsExactly(enCurso, futuroExacto, futuroEmpate);
    }

    private Long turno(Long profId, Long sillonId, Long prestacionId, Instant inicio, Instant fin, String estado) {
        return DatosAgenda.turno(jdbc, paciente, profId, sillonId, prestacionId, inicio.toString(), fin.toString(),
                estado);
    }

    private Long turno(Long profId, Long sillonId, Long prestacionId, String inicio, String fin, String estado) {
        return DatosAgenda.turno(jdbc, paciente, profId, sillonId, prestacionId, inicio, fin, estado);
    }
}
