package ar.edu.utn.turnos.profesionales.bloqueo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

class BloqueoJpaIT extends PruebaIntegracionBase {
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2030-06-10T10:00:00-03:00");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("PostgreSQL guarda ambos extremos como timestamptz")
    void usaTimestamptz() {
        var tipos = jdbc.queryForList("""
                select data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'bloqueo'
                  and column_name in ('inicio', 'fin')
                """, String.class);
        assertThat(tipos).containsExactly("timestamp with time zone", "timestamp with time zone");
    }

    @Test
    @Transactional
    @DisplayName("PostgreSQL rechaza un bloqueo con fin igual al inicio")
    void rechazaRangoInvalido() {
        Long profesional = jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', 'MP-BLOQUEO-JPA', true) returning id
                """, Long.class);
        assertThatThrownBy(() -> insertar(profesional, INICIO)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_bloqueo_intervalo");
    }

    @Test
    @DisplayName("PostgreSQL rechaza un bloqueo sin profesional existente")
    void rechazaProfesionalInexistente() {
        assertThatThrownBy(() -> insertar(-1L, INICIO.plusHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_bloqueo_profesional");
    }

    @Test
    @DisplayName("El script puede ejecutarse dos veces sin duplicar la FK de bloqueo")
    void creaFkUnaSolaVez() {
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema-constraints.sql"));
        script.setSeparator("^^^");
        script.execute(dataSource);
        script.execute(dataSource);
        Integer cantidad = jdbc.queryForObject("""
                select count(*) from pg_constraint
                where conname = 'fk_bloqueo_profesional' and conrelid = 'bloqueo'::regclass
                """, Integer.class);
        assertThat(cantidad).isEqualTo(1);
    }

    private void insertar(Long profesional, OffsetDateTime fin) {
        jdbc.update("insert into bloqueo (profesional_id, inicio, fin, motivo) values (?, ?, ?, ?)",
                profesional, INICIO, fin, "Congreso");
    }
}
