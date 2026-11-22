package ar.edu.utn.turnos.profesionales.horario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.LocalTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

class HorarioJpaIT extends PruebaIntegracionBase {
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime TRECE = LocalTime.of(13, 0);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("PostgreSQL guarda las horas del tramo como time")
    void usaTime() {
        var tipos = jdbc.queryForList("""
                select data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'horario_atencion'
                  and column_name in ('hora_inicio', 'hora_fin')
                """, String.class);
        assertThat(tipos).containsExactly("time without time zone", "time without time zone");
    }

    @Test
    @Transactional
    @DisplayName("PostgreSQL rechaza un tramo con fin igual al inicio")
    void rechazaTramoInvalido() {
        Long profesional = crearProfesional("MP-HORARIO-JPA-1");
        assertThatThrownBy(() -> insertar(profesional, 1, NUEVE, NUEVE))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_horario_atencion_tramo");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8})
    @Transactional
    @DisplayName("PostgreSQL rechaza un día de la semana fuera de 1 a 7")
    void rechazaDiaInvalido(int dia) {
        Long profesional = crearProfesional("MP-HORARIO-JPA-2");
        assertThatThrownBy(() -> insertar(profesional, dia, NUEVE, TRECE))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_horario_atencion_dia");
    }

    @Test
    @DisplayName("PostgreSQL rechaza un tramo sin profesional existente")
    void rechazaProfesionalInexistente() {
        assertThatThrownBy(() -> insertar(-1L, 1, NUEVE, TRECE))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_horario_atencion_profesional");
    }

    @Test
    @DisplayName("El script puede ejecutarse dos veces sin duplicar la FK de horario")
    void creaFkUnaSolaVez() {
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema-constraints.sql"));
        script.setSeparator("^^^");
        script.execute(dataSource);
        script.execute(dataSource);
        Integer cantidad = jdbc.queryForObject("""
                select count(*) from pg_constraint
                where conname = 'fk_horario_atencion_profesional' and conrelid = 'horario_atencion'::regclass
                """, Integer.class);
        assertThat(cantidad).isEqualTo(1);
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private void insertar(Long profesional, int dia, LocalTime inicio, LocalTime fin) {
        jdbc.update("insert into horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) values (?, ?, ?, ?)",
                profesional, dia, inicio, fin);
    }
}
