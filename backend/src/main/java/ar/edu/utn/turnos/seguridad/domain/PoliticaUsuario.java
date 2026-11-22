package ar.edu.utn.turnos.seguridad.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;

/** Reglas puras de la administración de usuarios (US-002, RN-33). */
public final class PoliticaUsuario {

    public static final String VINCULO_INVALIDO = "USUARIO_VINCULO_INVALIDO";
    public static final String ULTIMO_ADMIN = "USUARIO_ULTIMO_ADMIN";

    private PoliticaUsuario() {
    }

    /** Un odontólogo se vincula a exactamente un profesional; recepción y administración, a ninguno. */
    public static void validarVinculo(Rol rol, Long profesionalId) {
        if ((rol == Rol.ODONTOLOGO) != (profesionalId != null)) {
            throw new ReglaNegocioException(VINCULO_INVALIDO, EstadoError.NO_PROCESABLE_422,
                    "Un odontólogo debe estar vinculado a un profesional y los demás roles no.");
        }
    }

    /**
     * Impide quedarse sin administradores: si el usuario es ADMIN activo y deja de serlo, tiene que
     * existir otro ADMIN activo.
     *
     * @param adminsActivos cantidad de ADMIN activos, contando al usuario
     */
    public static void validarQuedaAdministrador(boolean eraAdminActivo, boolean seguiraAdminActivo, long adminsActivos) {
        if (eraAdminActivo && !seguiraAdminActivo && adminsActivos <= 1) {
            throw new ReglaNegocioException(ULTIMO_ADMIN, EstadoError.CONFLICTO_409,
                    "No se puede dejar el consultorio sin un administrador activo.");
        }
    }
}
