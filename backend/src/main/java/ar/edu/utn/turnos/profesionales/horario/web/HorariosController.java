package ar.edu.utn.turnos.profesionales.horario.web;

import ar.edu.utn.turnos.profesionales.horario.application.ServicioHorarios;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesionales/{id}/horarios")
class HorariosController {
    private final ServicioHorarios servicio;

    HorariosController(ServicioHorarios servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaTramo crear(@PathVariable Long id, @Valid @RequestBody SolicitudTramo solicitud) {
        return RespuestaTramo.de(servicio.crear(id, solicitud.tramo()));
    }

    @GetMapping
    List<RespuestaTramo> listar(@PathVariable Long id) {
        return servicio.listar(id).stream().map(RespuestaTramo::de).toList();
    }

    @PutMapping("/{horarioId}")
    RespuestaTramo reemplazar(@PathVariable Long id, @PathVariable Long horarioId,
            @Valid @RequestBody SolicitudTramo solicitud) {
        return RespuestaTramo.de(servicio.reemplazar(id, horarioId, solicitud.tramo()));
    }

    @DeleteMapping("/{horarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@PathVariable Long id, @PathVariable Long horarioId) {
        servicio.eliminar(id, horarioId);
    }
}
