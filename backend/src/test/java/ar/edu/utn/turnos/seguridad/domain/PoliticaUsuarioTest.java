package ar.edu.utn.turnos.seguridad.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PoliticaUsuarioTest {

    @Test
    @DisplayName("Un odontólogo exige profesional y los demás roles no lo admiten")
    void vinculoSegunRol() {
        assertThatCode(() -> PoliticaUsuario.validarVinculo(Rol.ODONTOLOGO, 7L)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaUsuario.validarVinculo(Rol.RECEPCION, null)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaUsuario.validarVinculo(Rol.ADMIN, null)).doesNotThrowAnyException();

        assertVinculoInvalido(() -> PoliticaUsuario.validarVinculo(Rol.ODONTOLOGO, null));
        assertVinculoInvalido(() -> PoliticaUsuario.validarVinculo(Rol.RECEPCION, 7L));
        assertVinculoInvalido(() -> PoliticaUsuario.validarVinculo(Rol.ADMIN, 7L));
    }

    @Test
    @DisplayName("Dejar de ser administrador activo exige que quede otro")
    void ultimoAdministrador() {
        assertThatThrownBy(() -> PoliticaUsuario.validarQuedaAdministrador(true, false, 1))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("USUARIO_ULTIMO_ADMIN");
                    assertThat(ex.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                });
        assertThatCode(() -> PoliticaUsuario.validarQuedaAdministrador(true, false, 2)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaUsuario.validarQuedaAdministrador(true, true, 1)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaUsuario.validarQuedaAdministrador(false, false, 0)).doesNotThrowAnyException();
    }

    private static void assertVinculoInvalido(org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
            assertThat(ex.codigo()).isEqualTo("USUARIO_VINCULO_INVALIDO");
            assertThat(ex.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
        });
    }
}
