package ar.edu.utn.turnos.listaespera.asignacion.web;

import ar.edu.utn.turnos.agenda.web.RespuestaTurno;
import ar.edu.utn.turnos.listaespera.asignacion.application.AsignarHuecoService;
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

/**
 * Asignación manual de un hueco a una entrada de la lista de espera (C-43, design D7/D8). Responde {@code 201}
 * con la representación del turno y {@code Location} al turno.
 */
@RestController
@RequestMapping("/api/lista-espera")
class AsignarHuecoController {
    private final AsignarHuecoService servicio;

    AsignarHuecoController(AsignarHuecoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/{id}/asignar")
    ResponseEntity<RespuestaTurno> asignar(@PathVariable Long id,
            @Valid @RequestBody SolicitudAsignarHueco solicitud,
            @AuthenticationPrincipal IdentidadPersonal identidad) {
        var turno = servicio.asignar(id, solicitud.dominio(), identidad != null ? identidad.id() : null);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/turnos/{id}").buildAndExpand(turno.id()).toUri();
        return ResponseEntity.created(ubicacion).body(RespuestaTurno.de(turno));
    }
}
