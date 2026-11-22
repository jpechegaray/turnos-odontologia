package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

class ProfesionalPrestacionJpaIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional
    @DisplayName("La base rechaza una pareja profesional-prestación duplicada")
    void rechazaParejaDuplicada() {
        Long profesional = crearProfesional("MP-PP-DUP");
        Long prestacion = crearPrestacion("Consulta DUP");
        insertar(profesional, prestacion);
        assertThatThrownBy(() -> insertar(profesional, prestacion))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una asociación con un profesional o una prestación inexistentes")
    void rechazaClavesForaneasInexistentes() {
        assertThatThrownBy(() -> insertar(-1L, -1L))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_profesional_prestacion_profesional");
        Long profesional = crearProfesional("MP-PP-FK");
        try {
            assertThatThrownBy(() -> insertar(profesional, -1L))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_profesional_prestacion_prestacion");
        } finally {
            jdbc.update("delete from profesional where id = ?", profesional);
        }
    }

    @Test
    @DisplayName("El script puede ejecutarse dos veces sin duplicar las FKs de profesional_prestacion")
    void creaFksUnaSolaVez() {
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema-constraints.sql"));
        script.setSeparator("^^^");
        script.execute(dataSource);
        script.execute(dataSource);
        Integer cantidad = jdbc.queryForObject("""
                select count(*) from pg_constraint
                where conname in ('fk_profesional_prestacion_profesional', 'fk_profesional_prestacion_prestacion')
                  and conrelid = 'profesional_prestacion'::regclass
                """, Integer.class);
        assertThat(cantidad).isEqualTo(2);
    }

    private void insertar(Long profesional, Long prestacion) {
        jdbc.update("insert into profesional_prestacion (profesional_id, prestacion_id) values (?, ?)",
                profesional, prestacion);
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private Long crearPrestacion(String nombre) {
        return jdbc.queryForObject("""
                insert into prestacion (nombre, nombre_clave, duracion_default_min, activa)
                values (?, ?, 30, true) returning id
                """, Long.class, nombre, nombre.toLowerCase());
    }
}
