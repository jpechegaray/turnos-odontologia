package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Verifica en PostgreSQL real que {@code schema-constraints.sql} crea las dos restricciones únicas
 * y el índice del planificador cuando faltan (convergencia de {@code ddl-auto=update} en dev) y que
 * los bloques son idempotentes.
 */
class NotificacionRestriccionesIT extends PruebaIntegracionBase {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    private void ejecutarScript() {
        ResourceDatabasePopulator populador = new ResourceDatabasePopulator(
                new ClassPathResource("schema-constraints.sql"));
        populador.setSeparator("^^^");
        populador.execute(dataSource);
    }

    private int restricciones(String nombre) {
        return jdbc.queryForObject(
                "select count(*) from pg_constraint where conname = ? and conrelid = 'notificacion_enviada'::regclass",
                Integer.class,
                nombre);
    }

    private int indices(String nombre) {
        return jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = ? and tablename = 'notificacion_enviada'",
                Integer.class,
                nombre);
    }

    @Test
    @DisplayName("El script crea las dos uniques y el índice de notificaciones si faltan")
    void elScriptCreaLasRestriccionesSiFaltan() {
        jdbc.execute("alter table notificacion_enviada drop constraint if exists uk_notificacion_turno_tipo_inicio");
        jdbc.execute("alter table notificacion_enviada drop constraint if exists uk_notificacion_oferta_tipo");
        jdbc.execute("drop index if exists ix_notificacion_estado_programada");

        assertThat(restricciones("uk_notificacion_turno_tipo_inicio")).isZero();
        assertThat(restricciones("uk_notificacion_oferta_tipo")).isZero();
        assertThat(indices("ix_notificacion_estado_programada")).isZero();

        ejecutarScript();

        assertThat(restricciones("uk_notificacion_turno_tipo_inicio")).isEqualTo(1);
        assertThat(restricciones("uk_notificacion_oferta_tipo")).isEqualTo(1);
        assertThat(indices("ix_notificacion_estado_programada")).isEqualTo(1);
    }

    @Test
    @DisplayName("El script de notificaciones es idempotente y no duplica restricciones ni índice")
    void elScriptEsIdempotente() {
        ejecutarScript();
        ejecutarScript();

        assertThat(restricciones("uk_notificacion_turno_tipo_inicio")).isEqualTo(1);
        assertThat(restricciones("uk_notificacion_oferta_tipo")).isEqualTo(1);
        assertThat(indices("ix_notificacion_estado_programada")).isEqualTo(1);
    }
}
