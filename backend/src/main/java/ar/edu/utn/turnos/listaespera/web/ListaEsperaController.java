package ar.edu.utn.turnos.listaespera.web;

import ar.edu.utn.turnos.listaespera.application.ServicioListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lista-espera")
class ListaEsperaController {
    private final ServicioListaEspera servicio;

    ListaEsperaController(ServicioListaEspera servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaEntrada crear(@Valid @RequestBody SolicitudAlta solicitud) {
        return RespuestaEntrada.de(servicio.crear(solicitud.datos()));
    }

    @GetMapping
    List<RespuestaEntrada> listar(@RequestParam(required = false) EstadoListaEspera estado,
            @RequestParam(required = false) Long pacienteId, @RequestParam(required = false) Long prestacionId) {
        return servicio.listar(estado, pacienteId, prestacionId).stream().map(RespuestaEntrada::de).toList();
    }

    @GetMapping("/{id}")
    RespuestaEntrada detalle(@PathVariable Long id) {
        return RespuestaEntrada.de(servicio.detalle(id));
    }

    /** La baja no borra: pasa la entrada a CANCELADA y la conserva. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(@PathVariable Long id) {
        servicio.cancelar(id);
    }
}
