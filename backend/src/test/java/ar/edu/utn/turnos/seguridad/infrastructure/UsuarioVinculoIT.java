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
class UsuarioVinculoIT extends PruebaIntegracionBase {

    private static final String HASH = new BCryptPasswordEncoder().encode("clave-fixture-c13");
    private static final Instant CREADO = Instant.parse("2026-11-22T12:00:00Z");

    @Autowired
    private UsuarioJpaRepository usuarios;

    @Test
    @DisplayName("La base rechaza dos usuarios vinculados al mismo profesional")
    void vinculoUnico() {
        usuarios.saveAndFlush(new Usuario("odonto-a@consultorio.test", HASH, "Odonto A", Rol.ODONTOLOGO, 501L, CREADO));

        assertThatThrownBy(() -> usuarios.saveAndFlush(
                new Usuario("odonto-b@consultorio.test", HASH, "Odonto B", Rol.ODONTOLOGO, 501L, CREADO)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(Usuario.RESTRICCION_PROFESIONAL);
    }

    @Test
    @DisplayName("Varios usuarios sin vínculo conviven y se modifican sin perder credenciales")
    void sinVinculoYModificacion() {
        Usuario admin = usuarios.saveAndFlush(new Usuario("admin2@consultorio.test", HASH, "Admin", Rol.ADMIN, CREADO));
        usuarios.saveAndFlush(new Usuario("recepcion2@consultorio.test", HASH, "Recepción", Rol.RECEPCION, CREADO));

        admin.modificar(" Jefa@Consultorio.Test ", "Jefa", Rol.ODONTOLOGO, 502L);
        admin.desactivar();
        usuarios.flush();

        Usuario leido = usuarios.findById(admin.getId()).orElseThrow();
        assertThat(leido.getEmail()).isEqualTo("jefa@consultorio.test");
        assertThat(leido.getNombre()).isEqualTo("Jefa");
        assertThat(leido.getRol()).isEqualTo(Rol.ODONTOLOGO);
        assertThat(leido.getProfesionalId()).isEqualTo(502L);
        assertThat(leido.isActivo()).isFalse();
        assertThat(leido.getPasswordHash()).isEqualTo(HASH);
        assertThat(usuarios.findAllByOrderByEmailAsc()).extracting(Usuario::getEmail)
                .containsSubsequence("jefa@consultorio.test", "recepcion2@consultorio.test");
    }
}
