package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.seguridad.application.UsuarioResumen;
import ar.edu.utn.turnos.seguridad.domain.ClaveUsuario;
import ar.edu.utn.turnos.seguridad.domain.EmailUsuario;
import ar.edu.utn.turnos.seguridad.domain.PoliticaUsuario;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de las cuentas del personal (US-002). Todo, incluso la lectura, es solo para ADMIN
 * (RN-33). Las contraseñas se guardan con BCrypt (RN-34) y nunca salen de este servicio.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class GestionUsuarios {

    static final String NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";
    static final String EMAIL_DUPLICADO = "USUARIO_EMAIL_DUPLICADO";
    static final String PROFESIONAL_YA_VINCULADO = "USUARIO_PROFESIONAL_YA_VINCULADO";

    private final UsuarioJpaRepository usuarios;
    private final ProfesionalConsulta profesionales;
    private final Clock reloj;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    GestionUsuarios(UsuarioJpaRepository usuarios, ProfesionalConsulta profesionales, Clock reloj) {
        this.usuarios = usuarios;
        this.profesionales = profesionales;
        this.reloj = reloj;
    }

    public List<UsuarioResumen> listar() {
        return usuarios.findAllByOrderByEmailAsc().stream().map(GestionUsuarios::resumir).toList();
    }

    public UsuarioResumen obtener(Long id) {
        return resumir(buscar(id));
    }

    @Transactional
    public UsuarioResumen crear(String email, String nombre, Rol rol, String clave, Long profesionalId) {
        if (!ClaveUsuario.esValida(clave)) {
            throw new IllegalArgumentException("La contraseña debe validarse antes de llegar al servicio");
        }
        String emailNormalizado = EmailUsuario.normalizar(email);
        PoliticaUsuario.validarVinculo(rol, profesionalId);
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw emailDuplicado();
        }
        if (profesionalId != null) {
            validarProfesionalDisponible(profesionalId, usuarios.existsByProfesionalId(profesionalId));
        }
        var usuario = new Usuario(emailNormalizado, bcrypt.encode(clave), nombre.strip(), rol, profesionalId, reloj.instant());
        return resumir(guardar(usuario));
    }

    @Transactional
    public UsuarioResumen modificar(Long id, String email, String nombre, Rol rol, Long profesionalId, boolean activo) {
        var usuario = buscar(id);
        String emailNormalizado = EmailUsuario.normalizar(email);
        PoliticaUsuario.validarVinculo(rol, profesionalId);
        if (usuarios.existsByEmailAndIdNot(emailNormalizado, id)) {
            throw emailDuplicado();
        }
        // Solo un vínculo nuevo exige un profesional activo: renombrar a un odontólogo cuyo profesional
        // se dio de baja después no debe fallar.
        if (profesionalId != null && !profesionalId.equals(usuario.getProfesionalId())) {
            validarProfesionalDisponible(profesionalId, usuarios.existsByProfesionalIdAndIdNot(profesionalId, id));
        }
        validarQuedaAdministrador(usuario, rol == Rol.ADMIN && activo);

        usuario.modificar(emailNormalizado, nombre.strip(), rol, profesionalId);
        if (activo) {
            usuario.reactivar();
        } else {
            usuario.desactivar();
        }
        return resumir(guardar(usuario));
    }

    /** Baja lógica e idempotente; el login ya rechaza a los inactivos y la sesión abierta se invalida. */
    @Transactional
    public void desactivar(Long id) {
        var usuario = buscar(id);
        if (!usuario.isActivo()) {
            return;
        }
        validarQuedaAdministrador(usuario, false);
        usuario.desactivar();
        guardar(usuario);
    }

    /**
     * Toma un bloqueo sobre los ADMIN activos antes de contarlos: dos administradores que se desactivan
     * a la vez quedan serializados y el segundo ve que ya no hay otro.
     */
    private void validarQuedaAdministrador(Usuario usuario, boolean seguiraAdminActivo) {
        boolean eraAdminActivo = usuario.getRol() == Rol.ADMIN && usuario.isActivo();
        if (eraAdminActivo && !seguiraAdminActivo) {
            long adminsActivos = usuarios.bloquearActivosConRol(Rol.ADMIN).size();
            PoliticaUsuario.validarQuedaAdministrador(true, false, adminsActivos);
        }
    }

    private void validarProfesionalDisponible(Long profesionalId, boolean yaVinculado) {
        boolean activo = profesionales.buscarPorId(profesionalId).map(ProfesionalConsulta.Datos::activo).orElse(false);
        if (!activo) {
            throw new ReglaNegocioException(PoliticaUsuario.VINCULO_INVALIDO, EstadoError.NO_PROCESABLE_422,
                    "El profesional indicado no existe o no está activo.");
        }
        if (yaVinculado) {
            throw profesionalYaVinculado();
        }
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ReglaNegocioException(NO_ENCONTRADO,
                EstadoError.NO_ENCONTRADO_404, "No existe el usuario solicitado."));
    }

    /** Si otra operación ganó la carrera sobre un UNIQUE, se responde el mismo 409 que la validación previa. */
    private Usuario guardar(Usuario usuario) {
        try {
            return usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            String detalle = Objects.toString(ex.getMostSpecificCause().getMessage(), "");
            throw detalle.contains(Usuario.RESTRICCION_PROFESIONAL) ? profesionalYaVinculado() : emailDuplicado();
        }
    }

    private static ReglaNegocioException emailDuplicado() {
        return new ReglaNegocioException(EMAIL_DUPLICADO, EstadoError.CONFLICTO_409,
                "Ya existe un usuario con ese email.");
    }

    private static ReglaNegocioException profesionalYaVinculado() {
        return new ReglaNegocioException(PROFESIONAL_YA_VINCULADO, EstadoError.CONFLICTO_409,
                "Ese profesional ya está vinculado a otro usuario.");
    }

    private static UsuarioResumen resumir(Usuario u) {
        return new UsuarioResumen(u.getId(), u.getEmail(), u.getNombre(), u.getRol(), u.getProfesionalId(), u.isActivo());
    }
}
