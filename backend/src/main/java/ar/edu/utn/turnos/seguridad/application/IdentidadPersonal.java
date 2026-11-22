package ar.edu.utn.turnos.seguridad.application;

import ar.edu.utn.turnos.seguridad.domain.Rol;

public record IdentidadPersonal(Long id, String email, Rol rol) {
}
