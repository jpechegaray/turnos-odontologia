package ar.edu.utn.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ScriptRestriccionesIT extends PruebaIntegracionBase {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("El script de restricciones es idempotente")
    void scriptDeRestriccionesEsIdempotente() {
        ResourceDatabasePopulator populador = new ResourceDatabasePopulator(
                new ClassPathResource("schema-constraints.sql"));
        populador.setSeparator("^^^");

        // Se ejecuta dos veces más sobre la base en la que la aplicación ya lo corrió al arrancar.
        assertThatCode(() -> {
            populador.execute(dataSource);
            populador.execute(dataSource);
        }).doesNotThrowAnyException();

        Integer cantidad = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Integer.class);
        assertThat(cantidad).isEqualTo(1);
    }
}
