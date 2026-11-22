package ar.edu.utn.turnos.seguridad.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClaveUsuarioTest {

    @Test
    @DisplayName("La contraseña inicial tiene entre 8 caracteres y 72 bytes UTF-8")
    void largoValido() {
        assertThat(ClaveUsuario.esValida("12345678")).isTrue();
        assertThat(ClaveUsuario.esValida("a".repeat(72))).isTrue();

        assertThat(ClaveUsuario.esValida("1234567")).isFalse();
        assertThat(ClaveUsuario.esValida("a".repeat(73))).isFalse();
        // 37 caracteres "ñ" ocupan 74 bytes en UTF-8: BCrypt ignoraría lo que supere 72.
        assertThat(ClaveUsuario.esValida("ñ".repeat(37))).isFalse();
        assertThat(ClaveUsuario.esValida(null)).isFalse();
    }
}
