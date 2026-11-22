package ar.edu.utn.turnos.profesionales.prestacion.web;

import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ServicioProfesionalPrestaciones;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesionales/{id}/prestaciones")
class ProfesionalPrestacionesController {

    private final ServicioProfesionalPrestaciones servicio;

    ProfesionalPrestacionesController(ServicioProfesionalPrestaciones servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    List<PrestacionResumen> listar(@PathVariable Long id) {
        return servicio.listar(id);
    }

    @PutMapping
    List<PrestacionResumen> reemplazar(@PathVariable Long id,
            @Valid @RequestBody AsignacionPrestaciones solicitud) {
        return servicio.reemplazar(id, solicitud.prestacionIds());
    }
}
