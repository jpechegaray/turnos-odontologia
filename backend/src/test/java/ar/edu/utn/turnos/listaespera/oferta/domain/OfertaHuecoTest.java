package ar.edu.utn.turnos.listaespera.oferta.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OfertaHuecoTest {
    private static final ZoneId BA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant LUNES_10 = LocalDateTime.parse("2026-12-07T10:00").atZone(BA).toInstant();
    private static final String HASH = "a".repeat(64);

    private static OfertaHueco nueva() {
        return OfertaHueco.nueva(1L, 2L, LUNES_10, LUNES_10.plus(Duration.ofMinutes(30)), 3L, HASH,
                LUNES_10.minus(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("La oferta nueva queda enviada y vence al inicio del hueco")
    void laOfertaNuevaQuedaEnviadaYVenceAlInicioDelHueco() {
        var oferta = nueva();

        assertThat(oferta.estado()).isEqualTo(EstadoOferta.ENVIADA);
        assertThat(oferta.expiraEn()).isEqualTo(LUNES_10);
        assertThat(oferta.vigente(LUNES_10.minusSeconds(1))).isTrue();
        assertThat(oferta.vigente(LUNES_10)).isFalse();
    }

    @Test
    @DisplayName("Un hueco vacío o invertido no es una oferta válida")
    void unHuecoVacioOInvertidoNoEsValido() {
        assertThatThrownBy(() -> OfertaHueco.nueva(1L, 2L, LUNES_10, LUNES_10, 3L, HASH, LUNES_10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rotar el token reemplaza el hash y conserva el resto")
    void rotarElTokenReemplazaElHash() {
        var rotada = nueva().conToken("b".repeat(64));

        assertThat(rotada.tokenHash()).isEqualTo("b".repeat(64));
        assertThat(rotada.estado()).isEqualTo(EstadoOferta.ENVIADA);
        assertThat(rotada.expiraEn()).isEqualTo(LUNES_10);
    }

    @Test
    @DisplayName("Una oferta tomada no está vigente")
    void unaOfertaTomadaNoEstaVigente() {
        var tomada = new OfertaHueco(9L, 1L, 2L, LUNES_10, LUNES_10.plusSeconds(1800), 3L, HASH, LUNES_10,
                EstadoOferta.TOMADA, LUNES_10.minusSeconds(60));

        assertThat(tomada.vigente(LUNES_10.minusSeconds(30))).isFalse();
    }
}
