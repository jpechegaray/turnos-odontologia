package ar.edu.utn.turnos.shared.paginacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaginaTest {

    @Test
    @DisplayName("Sin elementos hay cero páginas")
    void sinElementos() {
        assertThat(new Pagina<String>(List.of(), 0, 20, 0).totalPaginas()).isZero();
    }

    @Test
    @DisplayName("Una división exacta no suma una página de más")
    void divisionExacta() {
        assertThat(new Pagina<String>(List.of(), 0, 20, 20).totalPaginas()).isEqualTo(1);
        assertThat(new Pagina<String>(List.of(), 0, 20, 40).totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("El resto ocupa una página más")
    void conResto() {
        assertThat(new Pagina<String>(List.of(), 0, 20, 25).totalPaginas()).isEqualTo(2);
        assertThat(new Pagina<String>(List.of(), 0, 20, 1).totalPaginas()).isEqualTo(1);
    }

    @Test
    @DisplayName("map transforma el contenido y conserva página, tamaño y total")
    void mapConservaMetadatos() {
        Pagina<Integer> pagina = new Pagina<>(List.of(1, 2), 1, 2, 5);

        Pagina<String> transformada = pagina.map(n -> "n" + n);

        assertThat(transformada.contenido()).containsExactly("n1", "n2");
        assertThat(transformada.pagina()).isEqualTo(1);
        assertThat(transformada.tamanio()).isEqualTo(2);
        assertThat(transformada.totalElementos()).isEqualTo(5);
        assertThat(transformada.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("El contenido es una copia inmutable")
    void contenidoInmutable() {
        List<String> origen = new ArrayList<>(List.of("a"));
        Pagina<String> pagina = new Pagina<>(origen, 0, 20, 1);

        origen.add("b");

        assertThat(pagina.contenido()).containsExactly("a");
        assertThatThrownBy(() -> pagina.contenido().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Rechaza página negativa, tamaño menor a uno y total negativo")
    void rechazaValoresInvalidos() {
        assertThatThrownBy(() -> new Pagina<String>(List.of(), -1, 20, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pagina<String>(List.of(), 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pagina<String>(List.of(), 0, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
