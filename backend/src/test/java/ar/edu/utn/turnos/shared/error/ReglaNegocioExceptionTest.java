package ar.edu.utn.turnos.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReglaNegocioExceptionTest {

    @Test
    @DisplayName("Todos los estados de error admitidos son 4xx")
    void todosLosEstadosDeErrorSon4xx() {
        for (EstadoError estado : EstadoError.values()) {
            assertThat(estado.codigoHttp()).isBetween(400, 499);
        }
        assertThat(EstadoError.values()).extracting(EstadoError::codigoHttp)
                .contains(403, 404, 409, 422);
    }

    @Test
    @DisplayName("La excepción exige código y estado al construirse")
    void exigeCodigoYEstado() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReglaNegocioException(null, EstadoError.CONFLICTO_409, "m"));
        assertThatNullPointerException()
                .isThrownBy(() -> new ReglaNegocioException("RN-01", null, "m"));
    }

    @Test
    @DisplayName("Las propiedades de la excepción son inmutables y copiadas")
    void propiedadesSonInmutablesYCopiadas() {
        Map<String, Object> origen = new HashMap<>(Map.of("recurso", "SILLON"));
        ReglaNegocioException ex = new ReglaNegocioException("RN-02", EstadoError.CONFLICTO_409, "m", origen);
        origen.put("otra", "x");

        assertThat(ex.propiedades()).containsExactly(Map.entry("recurso", "SILLON"));
        assertThatThrownBy(() -> ex.propiedades().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(new ReglaNegocioException("RN-03", EstadoError.NO_ENCONTRADO_404, "m").propiedades()).isEmpty();
    }
}
