package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decide si una sesión abierta sigue representando al usuario tal como está ahora (C-13): existe, está
 * activo y conserva el rol y el email con los que inició sesión.
 */
@Component
class EstadoSesionPersonal {

    private final UsuarioJpaRepository usuarios;

    EstadoSesionPersonal(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Lectura por clave primaria y sin bloqueo: corre en cada request autenticado. */
    @Transactional(readOnly = true)
    boolean sigueVigente(IdentidadPersonal identidad) {
        return usuarios.findById(identidad.id())
                .filter(Usuario::isActivo)
                .filter(u -> u.getRol() == identidad.rol())
                .filter(u -> u.getEmail().equals(identidad.email()))
                .isPresent();
    }
}
