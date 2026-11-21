package ar.edu.utn.turnos.prestaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DuracionPrestacionTest {

    @ParameterizedTest(name = "{0} minutos")
    @ValueSource(ints = {5, 30, 45, 90, 480})
    @DisplayName("Las duraciones límite 5 y 480 se aceptan")
    void aceptaDuracionesValidas(int minutos) {
        assertThat(new DuracionPrestacion(minutos).minutos()).isEqualTo(minutos);
    }

    @Test
    @DisplayName("Una duración no múltiplo de 5 responde 422")
    void rechazaNoMultiploDeCinco() {
        assertRechazaConRn09(7);
    }

    @Test
    @DisplayName("Una duración mayor a 480 responde 422")
    void rechazaMayorA480() {
        assertRechazaConRn09(481);
        assertRechazaConRn09(485);
    }

    @Test
    @DisplayName("Una duración menor a 5 responde 422")
    void rechazaMenorA5() {
        assertRechazaConRn09(0);
        assertRechazaConRn09(-5);
    }

    private static void assertRechazaConRn09(int minutos) {
        assertThatThrownBy(() -> new DuracionPrestacion(minutos))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("RN-09");
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });
    }
}
