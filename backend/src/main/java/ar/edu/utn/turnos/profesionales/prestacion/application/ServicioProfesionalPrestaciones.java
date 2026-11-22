package ar.edu.utn.turnos.profesionales.prestacion.application;

import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.infrastructure.ProfesionalPrestacionesPersistencia;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioProfesionalPrestaciones {

    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";
    private static final String PUEDE_REEMPLAZAR = "hasRole('ADMIN')";
    private static final Comparator<PrestacionResumen> ORDEN_PRESTACIONES = Comparator
            .comparing(PrestacionResumen::nombre, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PrestacionResumen::id);

    private final ProfesionalPrestacionesPersistencia persistencia;
    private final ProfesionalConsulta profesionales;
    private final ServicioProfesionales catalogoProfesionales;
    private final ConsultaPrestaciones consultaPrestaciones;

    public ServicioProfesionalPrestaciones(
            ProfesionalPrestacionesPersistencia persistencia,
            ProfesionalConsulta profesionales,
            ServicioProfesionales catalogoProfesionales,
            ConsultaPrestaciones consultaPrestaciones) {
        this.persistencia = persistencia;
        this.profesionales = profesionales;
        this.catalogoProfesionales = catalogoProfesionales;
        this.consultaPrestaciones = consultaPrestaciones;
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<PrestacionResumen> listar(Long profesionalId) {
        catalogoProfesionales.detalle(profesionalId);
        return persistencia.prestacionIdsDe(profesionalId).stream()
                .map(consultaPrestaciones::buscarPorId)
                .flatMap(Optional::stream)
                .sorted(ORDEN_PRESTACIONES)
                .toList();
    }

    @PreAuthorize(PUEDE_REEMPLAZAR)
    @Transactional
    public List<PrestacionResumen> reemplazar(Long profesionalId, List<Long> prestacionIds) {
        verificarProfesional(profesionalId);
        Set<Long> deseadas = new LinkedHashSet<>(prestacionIds);
        Set<Long> actuales = persistencia.prestacionIdsDe(profesionalId);
        List<PrestacionResumen> resueltas = validarYResolver(deseadas, actuales);
        List<Long> altas = deseadas.stream().filter(id -> !actuales.contains(id)).toList();
        List<Long> bajas = actuales.stream().filter(id -> !deseadas.contains(id)).toList();
        try {
            persistencia.reemplazar(profesionalId, altas, bajas);
        } catch (DataIntegrityViolationException ex) {
            throw new ReglaNegocioException(
                    "PROFESIONAL_PRESTACIONES_CONFLICTO",
                    EstadoError.CONFLICTO_409,
                    "Conflicto al actualizar las prestaciones del profesional");
        }
        return resueltas.stream().sorted(ORDEN_PRESTACIONES).toList();
    }

    private List<PrestacionResumen> validarYResolver(Set<Long> deseadas, Set<Long> actuales) {
        List<PrestacionResumen> resultado = new ArrayList<>(deseadas.size());
        for (Long prestacionId : deseadas) {
            PrestacionResumen prestacion = consultaPrestaciones.buscarPorId(prestacionId)
                    .orElseThrow(() -> new ReglaNegocioException(
                            "PRESTACION_INEXISTENTE",
                            EstadoError.NO_PROCESABLE_422,
                            "No se encontró la prestación"));
            if (!prestacion.activa() && !actuales.contains(prestacionId)) {
                throw new ReglaNegocioException(
                        "PRESTACION_INACTIVA",
                        EstadoError.NO_PROCESABLE_422,
                        "No se puede asociar una prestación inactiva");
            }
            resultado.add(prestacion);
        }
        return resultado;
    }

    private void verificarProfesional(Long profesionalId) {
        if (profesionales.buscarPorId(profesionalId).isEmpty()) {
            throw new ReglaNegocioException("PROFESIONAL_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                    "No se encontró el profesional");
        }
    }
}
