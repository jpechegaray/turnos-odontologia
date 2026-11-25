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

    // --- C-19: solapamiento semiabierto [inicio, fin) (RN-01, RN-02) ---

    /** Instante de prueba: 10 de junio de 2030 a la hora UTC indicada ("HH:mm"). */
    private static Instant a(String hora) {
        return Instant.parse("2030-06-10T" + hora + ":00Z");
    }

    private static Intervalo de(String desde, String hasta) {
        return new Intervalo(a(desde), a(hasta));
    }

    @Test
    @DisplayName("Un intervalo que empieza justo cuando termina otro no se solapa, en ningún sentido")
    void bordeContiguoNoSolapa() {
        var existente = de("10:00", "10:30");
        var siguiente = de("10:30", "11:00");
        assertThat(siguiente.solapaCon(existente)).isFalse();
        assertThat(existente.solapaCon(siguiente)).isFalse();
    }

    @Test
    @DisplayName("Un intervalo que empieza un minuto antes del fin de otro se solapa")
    void unMinutoAntesSolapa() {
        var existente = de("10:00", "10:30");
        var nuevo = de("10:29", "10:59");
        assertThat(nuevo.solapaCon(existente)).isTrue();
        assertThat(existente.solapaCon(nuevo)).isTrue();
    }

    @Test
    @DisplayName("Un intervalo se solapa con otro igual, con uno que lo contiene y con uno contenido")
    void solapaPorInclusion() {
        var base = de("10:00", "11:00");
        assertThat(base.solapaCon(de("10:00", "11:00"))).isTrue();
        assertThat(base.solapaCon(de("09:00", "12:00"))).isTrue();
        assertThat(base.solapaCon(de("10:15", "10:45"))).isTrue();
    }

    @Test
    @DisplayName("Un intervalo que termina justo cuando empieza otro no se solapa")
    void bordeAnteriorNoSolapa() {
        assertThat(de("09:30", "10:00").solapaCon(de("10:00", "10:30"))).isFalse();
    }

    @Test
    @DisplayName("Un intervalo contiene a uno igual o interior, pero no a uno que sobresale")
    void contiene() {
        var tramo = de("09:00", "13:00");
        assertThat(tramo.contiene(de("09:00", "13:00"))).isTrue();
        assertThat(tramo.contiene(de("12:30", "13:00"))).isTrue();
        assertThat(tramo.contiene(de("12:45", "13:15"))).isFalse();
        assertThat(tramo.contiene(de("08:55", "09:30"))).isFalse();
    }

    @Test
    @DisplayName("Un intervalo creado desde un inicio y una duración termina inicio + duración")
    void desdeCalculaElFin() {
        var intervalo = Intervalo.desde(a("10:00"), 45);
        assertThat(intervalo.inicio()).isEqualTo(a("10:00"));
        assertThat(intervalo.fin()).isEqualTo(a("10:45"));
    }

    @Test
    @DisplayName("Un intervalo no se puede crear con una duración nula o negativa")
    void desdeRechazaDuracionNoPositiva() {
        assertThatIllegalArgumentException().isThrownBy(() -> Intervalo.desde(a("10:00"), 0));
        assertThatIllegalArgumentException().isThrownBy(() -> Intervalo.desde(a("10:00"), -5));
    }
}
