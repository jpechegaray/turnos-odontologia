package ar.edu.utn.turnos.profesionales.profesional.application;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta.Datos;
import ar.edu.utn.turnos.profesionales.profesional.infrastructure.ProfesionalesPersistencia;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioProfesionales {

    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";

    private final ProfesionalesPersistencia profesionales;
    private final Optional<ProfesionalVinculadoConsulta> vinculos;

    public ServicioProfesionales(ProfesionalesPersistencia profesionales,
            Optional<ProfesionalVinculadoConsulta> vinculos) {
        this.profesionales = profesionales;
        this.vinculos = vinculos;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Datos crear(String nombre, String apellido, String matricula, String especialidad) {
        return profesionales.crear(nombre, apellido, matricula, especialidad);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Datos actualizar(Long id, String nombre, String apellido, String matricula, String especialidad,
            boolean activo) {
        return profesionales.actualizar(id, nombre, apellido, matricula, especialidad, activo)
                .orElseThrow(ServicioProfesionales::noEncontrado);
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public Datos detalle(Long id) {
        var titular = limitarAlProfesionalPropio();
        if (titular.isPresent() && !titular.get().equals(id)) {
            throw accesoDenegado();
        }
        return profesionales.buscarPorId(id).orElseThrow(ServicioProfesionales::noEncontrado);
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<Datos> listar() {
        return limitarAlProfesionalPropio()
                .map(id -> profesionales.buscarPorId(id).stream().toList())
                .orElseGet(profesionales::listarTodos);
    }

    /** Solo los roles con lectura global devuelven vacío; un odontólogo sin vínculo se deniega. */
    private Optional<Long> limitarAlProfesionalPropio() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        boolean lecturaGlobal = autenticacion.getAuthorities().stream().anyMatch(autoridad ->
                "ROLE_ADMIN".equals(autoridad.getAuthority()) || "ROLE_RECEPCION".equals(autoridad.getAuthority()));
        if (lecturaGlobal) {
            return Optional.empty();
        }
        Long idPropio = vinculos.flatMap(consulta -> consulta.buscarIdPorIdentidad(autenticacion.getName()))
                .orElseThrow(ServicioProfesionales::accesoDenegado);
        return Optional.of(idPropio);
    }

    private static AccessDeniedException accesoDenegado() {
        return new AccessDeniedException("No tiene acceso al profesional solicitado");
    }

    private static ReglaNegocioException noEncontrado() {
        return new ReglaNegocioException("PROFESIONAL_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                "No se encontró el profesional");
    }
}
