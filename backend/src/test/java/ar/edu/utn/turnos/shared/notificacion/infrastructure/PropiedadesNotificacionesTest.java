package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validación de {@code app.notificaciones} con Bean Validation: remitente obligatorio y máximo de
 * intentos acotado a 1..3. Los valores ausentes toman los defaults del diseño.
 */
class PropiedadesNotificacionesTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private static PropiedadesNotificaciones validas() {
        return new PropiedadesNotificaciones(
                "smtp",
                "turnos@consultorio.test",
                new PropiedadesNotificaciones.Planificador(true, Duration.ofMinutes(5)),
                new PropiedadesNotificaciones.Reintento(3, Duration.ofMinutes(5), 2));
    }

    @Test
    @DisplayName("Las propiedades válidas no tienen violaciones")
    void lasPropiedadesValidasNoTienenViolaciones() {
        assertThat(validador.validate(validas())).isEmpty();
    }

    @Test
    @DisplayName("El remitente en blanco es inválido")
    void elRemitenteEnBlancoEsInvalido() {
        PropiedadesNotificaciones invalidas = new PropiedadesNotificaciones(
                "smtp", "   ", validas().planificador(), validas().reintento());

        assertThat(validador.validate(invalidas))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("remitente");
    }

    @Test
    @DisplayName("El máximo de intentos se acota a 1..3")
    void elMaximoDeIntentosSeAcota() {
        PropiedadesNotificaciones invalidas = new PropiedadesNotificaciones(
                "smtp",
                "turnos@consultorio.test",
                validas().planificador(),
                new PropiedadesNotificaciones.Reintento(4, Duration.ofMinutes(5), 2));

        assertThat(validador.validate(invalidas)).isNotEmpty();
    }

    @Test
    @DisplayName("Los valores ausentes toman los defaults del diseño")
    void losValoresAusentesTomanDefaults() {
        PropiedadesNotificaciones propiedades = new PropiedadesNotificaciones(
                null, "turnos@consultorio.test", null, null);

        assertThat(propiedades.canal()).isEqualTo("smtp");
        assertThat(propiedades.planificador().habilitado()).isTrue();
        assertThat(propiedades.planificador().intervalo()).isEqualTo(Duration.ofMinutes(5));
        assertThat(propiedades.reintento().maximoIntentos()).isEqualTo(3);
        assertThat(propiedades.reintento().esperaInicial()).isEqualTo(Duration.ofMinutes(5));
        assertThat(propiedades.reintento().multiplicador()).isEqualTo(2);
        assertThat(validador.validate(propiedades)).isEmpty();
    }
}
