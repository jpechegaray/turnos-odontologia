package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/turnos")
class TurnosController {
    private final CrearTurnoService crearTurno;

    TurnosController(CrearTurnoService crearTurno) {
        this.crearTurno = crearTurno;
    }

    /** Alta de un turno por el personal (US-010): 201, 400, 409 (RN-01) o 422 (RN-04, RN-09, RN-11, RN-13). */
    @PostMapping
    ResponseEntity<RespuestaTurno> crear(@Valid @RequestBody SolicitudCrearTurno solicitud,
            @AuthenticationPrincipal IdentidadPersonal identidad) {
        var turno = crearTurno.crear(solicitud.dominio(), solicitud.notas(), identidad != null ? identidad.id() : null);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(turno.id()).toUri();
        return ResponseEntity.created(ubicacion).body(RespuestaTurno.de(turno));
    }
}
