package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class GestionUsuariosTest {

    @Mock private UsuarioJpaRepository usuarios;
    @Mock private ProfesionalConsulta profesionales;
    private GestionUsuarios gestion;

    @BeforeEach
    void crear() {
        gestion = new GestionUsuarios(usuarios, profesionales, Clock.fixed(Instant.parse("2026-11-22T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Si otra alta gana la carrera por el email, el servicio responde 409 USUARIO_EMAIL_DUPLICADO")
    void carreraPorEmail() {
        when(usuarios.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("usuario_email_key"));

        assertThatThrownBy(() -> gestion.crear("c13@consultorio.test", "N", Rol.RECEPCION, "clave-fixture-c13", null))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        ex -> assertThat(ex.codigo()).isEqualTo("USUARIO_EMAIL_DUPLICADO"));
    }

    @Test
    @DisplayName("Si otra alta gana la carrera por el profesional, el servicio responde 409 USUARIO_PROFESIONAL_YA_VINCULADO")
    void carreraPorProfesional() {
        when(profesionales.buscarPorId(7L)).thenReturn(Optional.of(
                new ProfesionalConsulta.Datos(7L, "Ana", "Ficticia", "MP-7", null, true)));
        when(usuarios.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("violates unique constraint \"" + Usuario.RESTRICCION_PROFESIONAL + "\""));

        assertThatThrownBy(() -> gestion.crear("c13@consultorio.test", "N", Rol.ODONTOLOGO, "clave-fixture-c13", 7L))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        ex -> assertThat(ex.codigo()).isEqualTo("USUARIO_PROFESIONAL_YA_VINCULADO"));
    }
}
