package ar.edu.utn.turnos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParametroConfiguracionTest {

    @ParameterizedTest
    @CsvSource({"PLAZO_CANCELACION,0", "PLAZO_CANCELACION,168", "ANTICIPACION_RECORDATORIO,1",
        "ANTICIPACION_RECORDATORIO,168", "HORIZONTE_RESERVA,1", "HORIZONTE_RESERVA,2147483647",
        "ANTICIPACION_RESERVA,1", "TOPE_TURNOS_EMAIL,1", "TOPE_AVISOS,1", "RETENCION_ANIOS,1",
        "RETENCION_ANIOS,20"})
    @DisplayName("Los extremos válidos se normalizan a enteros")
    void aceptaExtremos(ParametroConfiguracion parametro, int valor) {
        assertThat(parametro.validar(BigDecimal.valueOf(valor))).isEqualTo(valor);
    }

    @ParameterizedTest
    @CsvSource({"PLAZO_CANCELACION,-1", "PLAZO_CANCELACION,169", "ANTICIPACION_RECORDATORIO,0",
        "ANTICIPACION_RECORDATORIO,169", "HORIZONTE_RESERVA,0", "HORIZONTE_RESERVA,2147483648",
        "ANTICIPACION_RESERVA,0", "TOPE_TURNOS_EMAIL,0", "TOPE_AVISOS,0", "RETENCION_ANIOS,0",
        "RETENCION_ANIOS,21"})
    @DisplayName("Los vecinos fuera de los límites incumplen RN-36")
    void rechazaFueraDeRango(ParametroConfiguracion parametro, String valor) {
        verificarRn36(parametro, new BigDecimal(valor));
    }

    @ParameterizedTest
    @EnumSource(value = ParametroConfiguracion.class, names = {"NOMBRE_CONSULTORIO", "DOMICILIO_CONSULTORIO",
        "EMAIL_CONTACTO_CONSULTORIO"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los parámetros numéricos no aceptan fracciones")
    void rechazaFracciones(ParametroConfiguracion parametro) {
        verificarRn36(parametro, new BigDecimal("12.5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \t", "\n"})
    @DisplayName("El nombre del consultorio no puede estar en blanco")
    void rechazaNombreBlanco(String valor) {
        verificarRn36(ParametroConfiguracion.NOMBRE_CONSULTORIO, valor);
    }

    @ParameterizedTest
    @EnumSource(value = ParametroConfiguracion.class, names = {"DOMICILIO_CONSULTORIO", "EMAIL_CONTACTO_CONSULTORIO"})
    @DisplayName("El domicilio y el email de contacto no pueden estar en blanco")
    void rechazaDatosDeContactoEnBlanco(ParametroConfiguracion parametro) {
        verificarRn36(parametro, "  ");
        assertThat(parametro.esNumerico()).isFalse();
    }

    private void verificarRn36(ParametroConfiguracion parametro, Object valor) {
        assertThatThrownBy(() -> parametro.validar(valor))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("RN-36");
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });
    }
}
