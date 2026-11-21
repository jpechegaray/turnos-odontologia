package ar.edu.utn.turnos.profesionales.sillon.infrastructure;

import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SillonesPersistencia implements SillonConsulta {

    private final SillonJpaRepository sillones;

    SillonesPersistencia(SillonJpaRepository sillones) {
        this.sillones = sillones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Datos> listarActivos() {
        return sillones.findByActivoTrueOrderById().stream().map(Sillon::datos).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Datos> buscarPorId(Long id) {
        return sillones.findById(id).map(Sillon::datos);
    }

    @Transactional(readOnly = true)
    public List<Datos> listarTodos() {
        return sillones.findAll(Sort.by("id")).stream().map(Sillon::datos).toList();
    }

    public Datos crear(String nombre, String descripcion) {
        return sillones.saveAndFlush(new Sillon(nombre, descripcion, true)).datos();
    }

    public Optional<Datos> actualizar(Long id, String nombre, String descripcion, boolean activo) {
        return sillones.findById(id).map(sillon -> {
            sillon.editar(nombre, descripcion, activo);
            sillones.flush();
            return sillon.datos();
        });
    }

    public static boolean esConflictoDeNombre(Throwable error) {
        for (Throwable causa = error.getCause(); causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException restriccion
                    && Sillon.RESTRICCION_NOMBRE.equals(restriccion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
