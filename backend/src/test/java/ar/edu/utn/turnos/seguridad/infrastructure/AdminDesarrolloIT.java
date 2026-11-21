package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminDesarrolloIT extends PruebaIntegracionBase {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    private static final String CLAVE = "clave-fixture-admin";
    private static final String ADMIN = "admin@consultorio.test";

    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("El administrador de desarrollo se inicializa una sola vez")
    void inicializaUnaVez() {
        new AdminDesarrollo(usuarios, RELOJ, CLAVE).run(null);
        usuarios.flush();
        String hash = hashGuardado();
        assertThat(new BCryptPasswordEncoder().matches(CLAVE, hash)).isTrue();

        new AdminDesarrollo(usuarios, RELOJ, "otra-clave-fixture").run(null);
        usuarios.flush();
        assertThat(hashGuardado()).isEqualTo(hash);
        assertThat(jdbc.queryForObject("select rol from usuario where email = ?", String.class, ADMIN))
                .isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("El administrador inicial no se crea fuera de desarrollo")
    void soloEnDesarrollo() {
        var contexto = new ApplicationContextRunner()
                .withBean(UsuarioJpaRepository.class, () -> mock(UsuarioJpaRepository.class))
                .withBean(Clock.class, () -> RELOJ)
                .withUserConfiguration(AdminDesarrollo.class)
                .withPropertyValues("SEED_ADMIN_PASSWORD=" + CLAVE);

        contexto.run(c -> assertThat(c).doesNotHaveBean(AdminDesarrollo.class));
        contexto.withPropertyValues("spring.profiles.active=dev").run(
                c -> assertThat(c).hasSingleBean(AdminDesarrollo.class));
    }

    @Test
    @DisplayName("Sin clave inicial no se crea el administrador de desarrollo")
    void noInicializaSinClave() {
        new AdminDesarrollo(usuarios, RELOJ, " ").run(null);
        usuarios.flush();

        assertThat(jdbc.queryForObject("select count(*) from usuario where email = ?", Long.class, ADMIN)).isZero();
    }

    private String hashGuardado() {
        return jdbc.queryForObject("select password_hash from usuario where email = ?", String.class, ADMIN);
    }
}
