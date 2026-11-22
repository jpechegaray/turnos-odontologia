package ar.edu.utn.turnos.profesionales.bloqueo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BloqueoConsultaIT extends PruebaIntegracionBase {
    private static final OffsetDateTime DIEZ = OffsetDateTime.parse("2030-06-10T10:00:00-03:00");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BloqueoConsulta consulta;

    private Long profesional;
    private Long otroProfesional;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional("MP-CONSULTA-BLOQUEO");
        otroProfesional = crearProfesional("MP-CONSULTA-OTRO");
        jdbc.update("insert into bloqueo (profesional_id, inicio, fin, motivo) values (?, ?, ?, ?)",
                profesional, DIEZ, DIEZ.plusHours(1), "Congreso");
    }

    @Test
    @DisplayName("El candidato que cruza un bloqueo del mismo profesional solapa")
    void candidatoSolapado() {
        assertThat(consulta.solapa(profesional, intervalo(30, 90))).isTrue();
    }

    @Test
    @DisplayName("El candidato contiguo a cualquiera de los extremos no solapa")
    void candidatosContiguos() {
        assertThat(consulta.solapa(profesional, intervalo(-30, 0))).isFalse();
        assertThat(consulta.solapa(profesional, intervalo(60, 90))).isFalse();
    }

    @Test
    @DisplayName("Los bloqueos de otro profesional no afectan la consulta")
    void distingueProfesional() {
        assertThat(consulta.solapa(otroProfesional, intervalo(30, 90))).isFalse();
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private static Intervalo intervalo(int inicio, int fin) {
        return new Intervalo(DIEZ.plusMinutes(inicio).toInstant(), DIEZ.plusMinutes(fin).toInstant());
    }
}
