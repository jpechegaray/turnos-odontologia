package ar.edu.utn.turnos.seguridad.application;

import ar.edu.utn.turnos.seguridad.domain.Rol;

/** Vista de un usuario del personal sin credenciales: nunca lleva la contraseña, su hash ni el estado de bloqueo. */
public record UsuarioResumen(Long id, String email, String nombre, Rol rol, Long profesionalId, boolean activo) {
}
