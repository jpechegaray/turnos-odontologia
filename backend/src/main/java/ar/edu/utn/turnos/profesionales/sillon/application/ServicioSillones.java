package ar.edu.utn.turnos.profesionales.sillon.application;

import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta.Datos;
import ar.edu.utn.turnos.profesionales.sillon.infrastructure.SillonesPersistencia;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioSillones {

    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";
    private static final String PUEDE_ESCRIBIR = "hasRole('ADMIN')";

    private final SillonesPersistencia sillones;

    public ServicioSillones(SillonesPersistencia sillones) {
        this.sillones = sillones;
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<Datos> listar() {
        return sillones.listarTodos();
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public Datos detalle(Long id) {
        return sillones.buscarPorId(id).orElseThrow(ServicioSillones::noEncontrado);
    }

    @PreAuthorize(PUEDE_ESCRIBIR)
    @Transactional
    public Datos crear(String nombre, String descripcion) {
        return persistir(() -> sillones.crear(nombre.strip(), descripcion));
    }

    @PreAuthorize(PUEDE_ESCRIBIR)
    @Transactional
    public Datos actualizar(Long id, String nombre, String descripcion, boolean activo) {
        return persistir(() -> {
            var anterior = sillones.buscarPorId(id).orElseThrow(ServicioSillones::noEncontrado);
            String nombreNormalizado = nombre.strip();
            if (anterior.activo() && !activo) {
                return desactivar(id, nombreNormalizado, descripcion);
            }
            return guardarCambios(id, nombreNormalizado, descripcion, activo);
        });
    }

    private Datos desactivar(Long id, String nombre, String descripcion) {
        return guardarCambios(id, nombre, descripcion, false);
    }

    private Datos guardarCambios(Long id, String nombre, String descripcion, boolean activo) {
        return sillones.actualizar(id, nombre, descripcion, activo).orElseThrow(ServicioSillones::noEncontrado);
    }

    private Datos persistir(Supplier<Datos> operacion) {
        try {
            return operacion.get();
        } catch (DataIntegrityViolationException error) {
            if (SillonesPersistencia.esConflictoDeNombre(error)) {
                throw new ReglaNegocioException("SILLON_NOMBRE_DUPLICADO", EstadoError.CONFLICTO_409,
                        "Ya existe un sillón con ese nombre");
            }
            throw error;
        }
    }

    private static ReglaNegocioException noEncontrado() {
        return new ReglaNegocioException("SILLON_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                "No se encontró el sillón");
    }
}
