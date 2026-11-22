package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UsuarioIT extends PruebaIntegracionBase {

    private static final String CLAVE_FICTICIA = "clave-fixture-c03";
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Autowired
    private UsuarioJpaRepository usuarios;

    @Test
    @DisplayName("El email se guarda normalizado y la contraseña solo como hash BCrypt")
    void normalizaEmailYGuardaHash() {
        Usuario usuario = usuarios.saveAndFlush(nuevoUsuario("Personal@Consultorio.Test"));

        assertThat(usuario.getEmail()).isEqualTo("personal@consultorio.test");
        assertThat(usuario.getPasswordHash()).isNotEqualTo(CLAVE_FICTICIA);
        assertThat(bcrypt.matches(CLAVE_FICTICIA, usuario.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Un email con otra capitalización no se puede guardar dos veces")
    void impideDuplicadosPorCapitalizacion() {
        usuarios.saveAndFlush(nuevoUsuario("Duplicado@Consultorio.Test"));

        assertThatThrownBy(() -> usuarios.saveAndFlush(nuevoUsuario("duplicado@consultorio.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario nuevoUsuario(String email) {
        return new Usuario(email, bcrypt.encode(CLAVE_FICTICIA), "Personal Ficticio",
                Rol.RECEPCION, Instant.parse("2026-11-21T12:00:00Z"));
    }
}
