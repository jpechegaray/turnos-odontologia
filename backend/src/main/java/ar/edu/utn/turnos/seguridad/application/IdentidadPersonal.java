package ar.edu.utn.turnos.seguridad.application;

import ar.edu.utn.turnos.seguridad.domain.Rol;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.security.Principal;

/**
 * Identidad del personal guardada en la sesión y devuelta por el login y {@code /api/auth/me}.
 * Como {@link Principal}, {@code Authentication.getName()} es el id del usuario, que no cambia aunque
 * el administrador le modifique el email (C-13). El JSON sigue siendo {@code {id, email, rol}}.
 */
public record IdentidadPersonal(Long id, String email, Rol rol) implements Principal {

    @Override
    @JsonIgnore
    public String getName() {
        return String.valueOf(id);
    }
}
