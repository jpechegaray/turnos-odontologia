package ar.edu.utn.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ExtensionBtreeGistIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("La extensión btree_gist queda instalada al arrancar la aplicación")
    void extensionBtreeGistQuedaInstalada() {
        Integer cantidad = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'btree_gist'", Integer.class);

        assertThat(cantidad).isEqualTo(1);
    }
}
