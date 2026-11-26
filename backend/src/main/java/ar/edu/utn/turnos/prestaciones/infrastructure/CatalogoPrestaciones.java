package ar.edu.utn.turnos.prestaciones.infrastructure;

import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Map;
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
    private final TurnosActivosConsulta turnosActivos;

    public CatalogoPrestaciones(PrestacionJpaRepository repositorio, TurnosActivosConsulta turnosActivos) {
        this.repositorio = repositorio;
        this.turnosActivos = turnosActivos;
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

    @Override
    public List<PrestacionResumen> listarActivas() {
        return listar(true);
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
        if (prestacion.isActiva() && !activa) {
            verificarSinTurnosFuturos(id);
        }
        prestacion.modificar(nombrePrestacion, duracion, activa);
        return resumir(guardar(prestacion));
    }

    /** Baja lógica (RN-37): repetirla no tiene efecto. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void darDeBaja(Long id) {
        var prestacion = buscar(id);
        if (prestacion.isActiva()) {
            verificarSinTurnosFuturos(id);
            prestacion.desactivar();
        }
    }

    private void verificarSinTurnosFuturos(Long id) {
        var conflictos = turnosActivos.futurosDePrestacion(id);
        if (!conflictos.isEmpty()) {
            throw new ReglaNegocioException(
                    "RN-37",
                    EstadoError.CONFLICTO_409,
                    "No se puede desactivar la prestación porque tiene turnos futuros activos",
                    Map.of("recurso", "PRESTACION", "turnos", conflictos));
        }
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
