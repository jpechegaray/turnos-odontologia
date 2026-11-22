package ar.edu.utn.turnos.prestaciones.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Reemplazo completo de una prestación; {@code activa = true} la reactiva. */
record PrestacionModificacion(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Integer duracionDefaultMin,
        @NotNull Boolean activa) {
}
