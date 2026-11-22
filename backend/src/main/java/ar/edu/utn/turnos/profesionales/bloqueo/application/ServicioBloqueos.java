package ar.edu.utn.turnos.profesionales.bloqueo.application;

import ar.edu.utn.turnos.profesionales.bloqueo.infrastructure.BloqueosPersistencia;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioBloqueos {
    private static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'RECEPCION')";
    private static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')";

    private final BloqueosPersistencia bloqueos;
    private final ProfesionalConsulta profesionales;
    private final ServicioProfesionales catalogo;

    public ServicioBloqueos(BloqueosPersistencia bloqueos, ProfesionalConsulta profesionales,
            ServicioProfesionales catalogo) {
        this.bloqueos = bloqueos;
        this.profesionales = profesionales;
        this.catalogo = catalogo;
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public BloqueoDatos crear(Long profesionalId, Intervalo intervalo, String motivo) {
        verificarProfesional(profesionalId);
        return bloqueos.crear(profesionalId, intervalo, motivo);
    }

    @PreAuthorize(PUEDE_CONSULTAR)
    @Transactional(readOnly = true)
    public List<BloqueoDatos> listar(Long profesionalId) {
        catalogo.detalle(profesionalId);
        return bloqueos.listar(profesionalId);
    }

    @PreAuthorize(PUEDE_GESTIONAR)
    @Transactional
    public void eliminar(Long profesionalId, Long bloqueoId) {
        verificarProfesional(profesionalId);
        if (!bloqueos.eliminar(profesionalId, bloqueoId)) {
            throw new ReglaNegocioException("BLOQUEO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                    "No se encontró el bloqueo");
        }
    }

    private void verificarProfesional(Long profesionalId) {
        if (profesionales.buscarPorId(profesionalId).isEmpty()) {
            throw new ReglaNegocioException("PROFESIONAL_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                    "No se encontró el profesional");
        }
    }
}
