package ar.edu.utn.turnos.profesionales.bloqueo.web;

import ar.edu.utn.turnos.profesionales.bloqueo.application.BloqueoDatos;
import ar.edu.utn.turnos.profesionales.bloqueo.application.ServicioBloqueos;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profesionales/{id}/bloqueos")
class BloqueosController {
    private final ServicioBloqueos servicio;

    BloqueosController(ServicioBloqueos servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BloqueoDatos crear(@PathVariable Long id, @Valid @RequestBody AltaBloqueo solicitud) {
        return servicio.crear(id, solicitud.intervalo(), solicitud.motivo());
    }

    @GetMapping
    List<BloqueoDatos> listar(@PathVariable Long id) {
        return servicio.listar(id);
    }

    @DeleteMapping("/{bloqueoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@PathVariable Long id, @PathVariable Long bloqueoId) {
        servicio.eliminar(id, bloqueoId);
    }
}
