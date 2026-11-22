package ar.edu.utn.turnos.prestaciones.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Nombre de una prestación sin espacios en los extremos. Su {@link #clave()} decide la unicidad:
 * no distingue mayúsculas ni espacios externos.
 */
public record NombrePrestacion(String valor) {

    private static final Locale ESPANOL = Locale.forLanguageTag("es-AR");

    public NombrePrestacion {
        valor = Objects.requireNonNull(valor, "valor").strip();
    }

    public String clave() {
        return valor.toLowerCase(ESPANOL);
    }
}
