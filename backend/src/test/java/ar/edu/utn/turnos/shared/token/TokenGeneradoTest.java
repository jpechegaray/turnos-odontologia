package ar.edu.utn.turnos.shared.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenGeneradoTest {

    @Test
    @DisplayName("El texto del token generado no contiene el token en claro ni su hash")
    void textoNoContieneTokenNiHash() {
        TokenGenerado token = new ServicioToken().generar();

        String texto = token.toString();

        assertThat(texto).doesNotContain(token.claro()).doesNotContain(token.hash()).contains("***");
    }

    @Test
    @DisplayName("Un token generado rechaza valores nulos")
    void rechazaValoresNulos() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenGenerado(null, "hash"));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenGenerado("claro", null));
    }
}
