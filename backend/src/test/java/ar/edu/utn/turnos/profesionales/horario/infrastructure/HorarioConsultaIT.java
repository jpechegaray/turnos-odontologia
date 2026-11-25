package ar.edu.utn.turnos.profesionales.horario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class HorarioConsultaIT extends PruebaIntegracionBase {
    /** 2030-06-10 es lunes; las horas se expresan en Buenos Aires. */
    private static final String LUNES = "2030-06-10T%s:00-03:00";
    private static final String MARTES = "2030-06-11T%s:00-03:00";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private HorarioConsulta consulta;

    private Long profesional;
    private Long otroProfesional;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional("MP-CONSULTA-HORARIO");
        otroProfesional = crearProfesional("MP-CONSULTA-HORARIO-OTRO");
        guardarTramo(profesional, 1, "09:00", "13:00");
    }

    @Test
    @DisplayName("Un turno que termina justo al cierre del tramo queda contenido")
    void terminaAlCierre() {
        assertThat(consulta.contiene(profesional, intervalo(LUNES, "12:30", "13:00"))).isTrue();
    }

    @Test
    @DisplayName("Un turno que excede el cierre del tramo no queda contenido")
    void excedeElCierre() {
        assertThat(consulta.contiene(profesional, intervalo(LUNES, "12:45", "13:15"))).isFalse();
    }

    @Test
    @DisplayName("Un turno que cruza el corte entre dos tramos no queda contenido")
    void cruzaElCorte() {
        guardarTramo(profesional, 1, "13:00", "17:00");
        assertThat(consulta.contiene(profesional, intervalo(LUNES, "12:45", "13:15"))).isFalse();
    }

    @Test
    @DisplayName("Un turno en otro día o de otro profesional no queda contenido")
    void otroDiaUOtroProfesional() {
        assertThat(consulta.contiene(profesional, intervalo(MARTES, "10:00", "10:30"))).isFalse();
        assertThat(consulta.contiene(otroProfesional, intervalo(LUNES, "10:00", "10:30"))).isFalse();
    }

    @Test
    @DisplayName("La contención se calcula en hora de Buenos Aires")
    void calculaEnBuenosAires() {
        var candidato = new Intervalo(Instant.parse("2030-06-10T12:00:00Z"), Instant.parse("2030-06-10T12:30:00Z"));
        assertThat(consulta.contiene(profesional, candidato)).isTrue();
    }

    @Test
    @DisplayName("Los tramos se listan por profesional o de todos los profesionales")
    void listaTramos() {
        guardarTramo(otroProfesional, 3, "14:00", "18:00");

        assertThat(consulta.tramos(profesional))
                .extracting(h -> h.profesionalId() + "-" + h.diaSemana() + "-" + h.horaInicio() + "-" + h.horaFin())
                .containsExactly(profesional + "-1-09:00-13:00");
        assertThat(consulta.tramos(null))
                .extracting(h -> h.profesionalId() + "-" + h.diaSemana() + "-" + h.horaInicio())
                .contains(profesional + "-1-09:00", otroProfesional + "-3-14:00");
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private void guardarTramo(Long id, int dia, String inicio, String fin) {
        jdbc.update("insert into horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) values (?, ?, ?, ?)",
                id, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private static Intervalo intervalo(String dia, String inicio, String fin) {
        return new Intervalo(OffsetDateTime.parse(dia.formatted(inicio)).toInstant(),
                OffsetDateTime.parse(dia.formatted(fin)).toInstant());
    }
}
