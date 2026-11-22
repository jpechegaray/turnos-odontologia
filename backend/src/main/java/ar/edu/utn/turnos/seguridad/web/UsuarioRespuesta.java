package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.application.UsuarioResumen;
import ar.edu.utn.turnos.seguridad.domain.Rol;

/** Contrato de salida de {@code /api/usuarios}: nunca incluye credenciales ni el estado de bloqueo. */
record UsuarioRespuesta(Long id, String email, String nombre, Rol rol, Long profesionalId, boolean activo) {

    static UsuarioRespuesta de(UsuarioResumen u) {
        return new UsuarioRespuesta(u.id(), u.email(), u.nombre(), u.rol(), u.profesionalId(), u.activo());
    }
}
