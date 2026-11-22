package ar.edu.utn.turnos.prestaciones.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NombrePrestacionTest {

    @Test
    @DisplayName("El nombre se guarda sin espacios en los extremos")
    void recortaEspacios() {
        assertThat(new NombrePrestacion("  Limpieza  ").valor()).isEqualTo("Limpieza");
    }

    @Test
    @DisplayName("Nombres que difieren en mayúsculas o espacios externos comparten la clave de unicidad")
    void claveIgnoraMayusculasYEspacios() {
        assertThat(new NombrePrestacion(" limpieza ").clave())
                .isEqualTo(new NombrePrestacion("Limpieza").clave())
                .isEqualTo("limpieza");
    }

    @Test
    @DisplayName("La clave conserva los acentos y la eñe")
    void claveConservaAcentos() {
        assertThat(new NombrePrestacion("EXTRACCIÓN Ñ").clave()).isEqualTo("extracción ñ");
    }
}
