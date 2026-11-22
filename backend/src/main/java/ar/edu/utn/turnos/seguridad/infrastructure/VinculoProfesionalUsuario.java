package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el profesional del odontólogo autenticado para C-06. La identidad es
 * {@code Authentication.getName()}, que para el personal es el id del usuario (IdentidadPersonal).
 * El vínculo se lee en cada consulta, así un cambio del administrador rige de inmediato.
 * Lo registra {@link VinculoProfesionalAutoConfiguration}, no el escaneo de componentes.
 */
class VinculoProfesionalUsuario implements ProfesionalVinculadoConsulta {

    private final UsuarioJpaRepository usuarios;

    VinculoProfesionalUsuario(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarIdPorIdentidad(String identidad) {
        return idDeUsuario(identidad)
                .flatMap(usuarios::findById)
                .filter(Usuario::isActivo)
                .map(Usuario::getProfesionalId);
    }

    /** Una identidad que no es un id (por ejemplo, un usuario simulado en tests) no tiene vínculo. */
    private static Optional<Long> idDeUsuario(String identidad) {
        if (identidad == null || !identidad.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(identidad));
    }
}
