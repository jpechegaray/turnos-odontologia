package ar.edu.utn.turnos.prestaciones.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de prestación. El rango y el múltiplo de 5 (RN-09) los valida el dominio con 422. */
record PrestacionSolicitud(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull Integer duracionDefaultMin) {
}
