package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.domain.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta de usuario. El vínculo según el rol y la unicidad los valida el servicio (422/409). */
record UsuarioAlta(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 255) String nombre,
        @NotNull Rol rol,
        @ClaveValida String password,
        Long profesionalId) {

    /** La contraseña nunca llega a un log. */
    @Override
    public String toString() {
        return "UsuarioAlta[email=" + email + ", rol=" + rol + ", password=redactada]";
    }
}
