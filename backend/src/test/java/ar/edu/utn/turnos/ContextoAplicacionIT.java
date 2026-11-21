package ar.edu.utn.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

class ContextoAplicacionIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Environment entorno;

    @Test
    @DisplayName("El contexto de la aplicación carga con el perfil test sobre PostgreSQL real")
    void contextoCargaConPerfilTestSobrePostgresReal() {
        assertThat(entorno.getActiveProfiles()).containsExactly("test");
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL 16");
    }

    @Test
    @DisplayName("El perfil test arranca con un esquema limpio")
    void perfilTestArrancaConEsquemaLimpio() {
        assertThat(entorno.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
    }
}
