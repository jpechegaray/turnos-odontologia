package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la asistencia o ausencia de un turno (C-29; RN-12, RN-20). Lee el turno con lock de fila, así dos
 * marcas simultáneas se serializan: la segunda ve la marca ya hecha y entra por la rama de corrección (solo
 * ADMIN) o recibe 409. Orden de chequeos: 404, CANCELADO 409, corrección por no-ADMIN 403, misma marca 409,
 * turno no empezado 422.
 */
@Service
public class MarcarAsistenciaService {
    private final TurnosPersistencia turnos;
    private final Clock reloj;

    public MarcarAsistenciaService(TurnosPersistencia turnos, Clock reloj) {
        this.turnos = turnos;
        this.reloj = reloj;
    }

    /**
     * Marca o corrige la asistencia. El rol llega del controlador (identidad de la sesión) para que el servicio
     * no dependa del {@code SecurityContext} al decidir la corrección.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno marcar(Long turnoId, EstadoTurno destino, Rol rol) {
        var turno = turnos.buscarParaActualizar(turnoId).orElseThrow(() -> new ReglaNegocioException(
                "TURNO_INEXISTENTE", EstadoError.NO_ENCONTRADO_404, "No existe el turno " + turnoId));
        Turno marcado;
        if (esMarcaDeAsistencia(turno.estado())) {
            if (rol != Rol.ADMIN) {
                throw new AccessDeniedException("Solo el administrador corrige una marca de asistencia");
            }
            marcado = turno.corregirAsistencia(destino);
        } else {
            // CANCELADO también es final, pero no es una marca: pasarA lo rechaza con 409 para cualquier rol.
            marcado = turno.marcarAsistencia(destino, Instant.now(reloj));
        }
        return turnos.guardar(marcado);
    }

    private static boolean esMarcaDeAsistencia(EstadoTurno estado) {
        return estado == EstadoTurno.ATENDIDO || estado == EstadoTurno.AUSENTE;
    }
}
