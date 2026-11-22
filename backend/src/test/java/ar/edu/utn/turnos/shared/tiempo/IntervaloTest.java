package ar.edu.utn.turnos.shared.tiempo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IntervaloTest {
    private static final Instant INICIO = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant FIN = INICIO.plusSeconds(3600);

    @Test
    @DisplayName("Un intervalo conserva sus extremos crecientes")
    void conservaExtremos() {
        var intervalo = new Intervalo(INICIO, FIN);
        assertThat(intervalo.inicio()).isEqualTo(INICIO);
        assertThat(intervalo.fin()).isEqualTo(FIN);
    }

    @Test
    @DisplayName("Un intervalo rechaza extremos iguales o invertidos")
    void rechazaExtremosInvalidos() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Intervalo(INICIO, INICIO));
        assertThatIllegalArgumentException().isThrownBy(() -> new Intervalo(FIN, INICIO));
    }
}
