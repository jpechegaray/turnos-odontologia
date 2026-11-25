package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.CancelarTurnoService;
import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.agenda.application.MarcarAsistenciaService;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/turnos")
class TurnosController {
    private final CrearTurnoService crearTurno;
    private final CancelarTurnoService cancelarTurno;
    private final MarcarAsistenciaService marcarAsistencia;

    TurnosController(CrearTurnoService crearTurno, CancelarTurnoService cancelarTurno,
            MarcarAsistenciaService marcarAsistencia) {
        this.crearTurno = crearTurno;
        this.cancelarTurno = cancelarTurno;
        this.marcarAsistencia = marcarAsistencia;
    }

    /** Alta de un turno por el personal (US-010): 201, 400, 409 (RN-01) o 422 (RN-04, RN-09, RN-11, RN-13). */
    @PostMapping
    ResponseEntity<RespuestaTurno> crear(@Valid @RequestBody SolicitudCrearTurno solicitud,
            @AuthenticationPrincipal IdentidadPersonal identidad) {
        var turno = crearTurno.crear(solicitud.dominio(), solicitud.notas(), identidad != null ? identidad.id() : null);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(turno.id()).toUri();
        return ResponseEntity.created(ubicacion).body(RespuestaTurno.de(turno));
    }

    /**
     * Cancelación por el personal (US-012): 200, 400 (motivo largo), 404, 409 (estado final o conflicto de
     * versión) o 422 (RN-05, turno ya iniciado). El cuerpo es opcional.
     */
    @PostMapping("/{id}/cancelar")
    RespuestaTurno cancelar(@PathVariable Long id,
            @Valid @RequestBody(required = false) SolicitudCancelarTurno solicitud,
            @AuthenticationPrincipal IdentidadPersonal identidad) {
        var motivo = solicitud != null ? solicitud.motivo() : null;
        return RespuestaTurno.de(cancelarTurno.cancelarPorPersonal(id, motivo, identidad != null ? identidad.id() : null));
    }

    /**
     * Registro o corrección de asistencia (C-29; RN-12, RN-20): 200, 400 (estado ausente o distinto de ATENDIDO y
     * AUSENTE), 403 (corrección por un rol que no es ADMIN), 404, 409 (cancelado, misma marca o conflicto de
     * versión) o 422 (RN-20, turno que todavía no empezó). El cuerpo es obligatorio.
     */
    @PostMapping("/{id}/asistencia")
    RespuestaTurno marcarAsistencia(@PathVariable Long id, @Valid @RequestBody SolicitudMarcarAsistencia solicitud,
            @AuthenticationPrincipal IdentidadPersonal identidad) {
        var rol = identidad != null ? identidad.rol() : null;
        return RespuestaTurno.de(marcarAsistencia.marcar(id, solicitud.estado(), rol));
    }
}
