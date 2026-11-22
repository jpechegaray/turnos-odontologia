package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Reemplazo completo de un usuario, sin contraseña. {@code activo = true} lo reactiva. */
record UsuarioModificacion(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 255) String nombre,
        @NotNull Rol rol,
        Long profesionalId,
        @NotNull Boolean activo) {
}
