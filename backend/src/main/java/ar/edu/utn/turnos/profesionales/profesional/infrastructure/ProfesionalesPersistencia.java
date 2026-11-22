package ar.edu.utn.turnos.profesionales.profesional.infrastructure;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProfesionalesPersistencia implements ProfesionalConsulta {

    private final ProfesionalJpaRepository profesionales;

    ProfesionalesPersistencia(ProfesionalJpaRepository profesionales) {
        this.profesionales = profesionales;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datos> listarActivos() {
        return profesionales.findByActivoTrueOrderById().stream().map(Profesional::datos).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Datos> buscarPorId(Long id) {
        return profesionales.findById(id).map(Profesional::datos);
    }

    public List<Datos> listarTodos() {
        return profesionales.findAll(Sort.by("id")).stream().map(Profesional::datos).toList();
    }

    public Datos crear(String nombre, String apellido, String matricula, String especialidad) {
        return persistir(() -> profesionales.saveAndFlush(new Profesional(nombre, apellido, matricula,
                especialidad, true)).datos());
    }

    public Optional<Datos> actualizar(Long id, String nombre, String apellido, String matricula, String especialidad,
            boolean activo) {
        return profesionales.findById(id).map(profesional -> persistir(() -> {
            profesional.actualizar(nombre, apellido, matricula, especialidad);
            if (profesional.isActivo() && !activo) {
                profesional.desactivar();
            } else if (!profesional.isActivo() && activo) {
                profesional.reactivar();
            }
            return profesionales.saveAndFlush(profesional).datos();
        }));
    }

    private Datos persistir(Supplier<Datos> escritura) {
        try {
            return escritura.get();
        } catch (DataIntegrityViolationException error) {
            if (error.getCause() instanceof ConstraintViolationException restriccion
                    && Profesional.RESTRICCION_MATRICULA.equals(restriccion.getConstraintName())) {
                throw new ReglaNegocioException("PROFESIONAL_MATRICULA_DUPLICADA", EstadoError.CONFLICTO_409,
                        "Ya existe un profesional con esa matrícula");
            }
            throw error;
        }
    }
}
