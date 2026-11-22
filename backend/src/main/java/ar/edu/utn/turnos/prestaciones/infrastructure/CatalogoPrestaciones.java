package ar.edu.utn.turnos.prestaciones.infrastructure;

import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catálogo de prestaciones (US-003). Solo ADMIN escribe (RN-33); las lecturas no llevan anotación
 * porque la API ya exige sesión por ruta y el puerto {@link ConsultaPrestaciones} sirve a flujos públicos.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoPrestaciones implements ConsultaPrestaciones {

    static final String DUPLICADA = "PRESTACION-DUPLICADA";
    static final String NO_ENCONTRADA = "PRESTACION-NO-ENCONTRADA";

    private final PrestacionJpaRepository repositorio;

    public CatalogoPrestaciones(PrestacionJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<PrestacionResumen> listar(boolean soloActivas) {
        var prestaciones = soloActivas
                ? repositorio.findByActivaTrueOrderByNombreClaveAsc()
                : repositorio.findAllByOrderByNombreClaveAsc();
        return prestaciones.stream().map(CatalogoPrestaciones::resumir).toList();
    }

    public PrestacionResumen obtener(Long id) {
        return resumir(buscar(id));
    }

    @Override
    public Optional<PrestacionResumen> buscarPorId(Long id) {
        return repositorio.findById(id).map(CatalogoPrestaciones::resumir);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PrestacionResumen crear(String nombre, int duracionDefaultMin) {
        var nombrePrestacion = new NombrePrestacion(nombre);
        var duracion = new DuracionPrestacion(duracionDefaultMin);
        if (repositorio.existsByNombreClave(nombrePrestacion.clave())) {
            throw duplicada();
        }
        return resumir(guardar(new Prestacion(nombrePrestacion, duracion)));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public PrestacionResumen modificar(Long id, String nombre, int duracionDefaultMin, boolean activa) {
        var prestacion = buscar(id);
        var nombrePrestacion = new NombrePrestacion(nombre);
        var duracion = new DuracionPrestacion(duracionDefaultMin);
        if (repositorio.existsByNombreClaveAndIdNot(nombrePrestacion.clave(), id)) {
            throw duplicada();
        }
        prestacion.modificar(nombrePrestacion, duracion, activa);
        return resumir(guardar(prestacion));
    }

    /** Baja lógica (RN-37): repetirla no tiene efecto. Las guardas con turnos futuros llegan en C-30. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void darDeBaja(Long id) {
        buscar(id).desactivar();
    }

    private Prestacion buscar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ReglaNegocioException(NO_ENCONTRADA,
                EstadoError.NO_ENCONTRADO_404, "No existe la prestación solicitada."));
    }

    /**
     * El dominio ya validó RN-09, así que la única violación de integridad posible es el UNIQUE de
     * {@code nombre_clave} cuando otra alta simultánea ganó la carrera.
     */
    private Prestacion guardar(Prestacion prestacion) {
        try {
            return repositorio.saveAndFlush(prestacion);
        } catch (DataIntegrityViolationException ex) {
            throw duplicada();
        }
    }

    private static ReglaNegocioException duplicada() {
        return new ReglaNegocioException(DUPLICADA, EstadoError.CONFLICTO_409,
                "Ya existe una prestación con ese nombre.");
    }

    private static PrestacionResumen resumir(Prestacion prestacion) {
        return new PrestacionResumen(prestacion.getId(), prestacion.getNombre(),
                prestacion.getDuracionDefaultMin(), prestacion.isActiva());
    }
}
