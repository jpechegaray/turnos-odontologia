package ar.edu.utn.turnos.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LimitadorPorClaveTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");

    @Test
    @DisplayName("El limitador por clave bloquea al superar el tope para una misma clave normalizada")
    void elLimitadorPorClaveBloqueaAlSuperarElTopeParaUnaMismaClaveNormalizada() {
        var reloj = new LimitadorTasaEnMemoriaTest.RelojMutable(T0);
        var props = new RateLimitProperties(60, Duration.ofMinutes(1), 2, Duration.ofSeconds(45));
        var limitador = new LimitadorPorClave(props, reloj);

        assertThat(limitador.evaluar("ana.ficticia@consultorio.test").permitido()).isTrue();
        assertThat(limitador.evaluar("  ANA.FICTICIA@CONSULTORIO.TEST ").permitido()).isTrue();

        var rechazado = limitador.evaluar("Ana.Ficticia@consultorio.test");
        assertThat(rechazado.permitido()).isFalse();
        assertThat(rechazado.segundosReintento()).isEqualTo(45L);

        assertThatThrownBy(() -> limitador.verificarOConsumir("  ana.ficticia@consultorio.test  "))
                .isInstanceOf(LimiteTasaSuperadoException.class)
                .satisfies(ex -> {
                    var tasaEx = (LimiteTasaSuperadoException) ex;
                    assertThat(tasaEx.codigo()).isEqualTo("RN-27");
                    assertThat(tasaEx.segundosReintento()).isEqualTo(45L);
                    assertThat(tasaEx.getMessage())
                            .doesNotContainIgnoringCase("ana")
                            .doesNotContainIgnoringCase("ficticia")
                            .doesNotContainIgnoringCase("consultorio.test");
                    assertThat(tasaEx.toString())
                            .doesNotContainIgnoringCase("ana")
                            .doesNotContainIgnoringCase("ficticia")
                            .doesNotContainIgnoringCase("consultorio.test");
                });
    }

    @Test
    @DisplayName("Dos claves distintas tienen cupos independientes y se reinician al vencer su ventana")
    void dosClavesDistintasTienenCuposIndependientesYSeReinicianAlVencerSuVentana() {
        var reloj = new LimitadorTasaEnMemoriaTest.RelojMutable(T0);
        var props = new RateLimitProperties(60, Duration.ofMinutes(1), 1, Duration.ofSeconds(30));
        var limitador = new LimitadorPorClave(props, reloj);

        limitador.verificarOConsumir("ana.ficticia@consultorio.test");
        assertThat(limitador.evaluar("ana.ficticia@consultorio.test").permitido()).isFalse();

        assertThat(limitador.evaluar("bruno.inventado@consultorio.test").permitido()).isTrue();

        reloj.avanzar(Duration.ofSeconds(30));
        assertThat(limitador.evaluar("ana.ficticia@consultorio.test").permitido()).isTrue();
    }

    @Test
    @DisplayName("Las propiedades por defecto aplican valores positivos si se omiten")
    void propiedadesPorDefectoAplicanValoresPositivos() {
        var props = RateLimitProperties.porDefecto();
        assertThat(props.maxSolicitudesPorIp()).isPositive();
        assertThat(props.ventanaIp()).isPositive();
        assertThat(props.maxSolicitudesPorClave()).isPositive();
        assertThat(props.ventanaClave()).isPositive();
    }
}
