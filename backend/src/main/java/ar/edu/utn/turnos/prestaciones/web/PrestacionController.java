package ar.edu.utn.turnos.prestaciones.web;

import ar.edu.utn.turnos.prestaciones.infrastructure.CatalogoPrestaciones;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Catálogo de prestaciones para el personal (US-003). Los permisos los aplica el servicio. */
@RestController
@RequestMapping("/api/prestaciones")
public class PrestacionController {

    private final CatalogoPrestaciones catalogo;

    public PrestacionController(CatalogoPrestaciones catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    List<PrestacionRespuesta> listar(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return catalogo.listar(soloActivas).stream().map(PrestacionRespuesta::de).toList();
    }

    @GetMapping("/{id}")
    PrestacionRespuesta obtener(@PathVariable Long id) {
        return PrestacionRespuesta.de(catalogo.obtener(id));
    }

    @PostMapping
    ResponseEntity<PrestacionRespuesta> crear(@Valid @RequestBody PrestacionSolicitud solicitud) {
        var creada = PrestacionRespuesta.de(catalogo.crear(solicitud.nombre(), solicitud.duracionDefaultMin()));
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @PutMapping("/{id}")
    PrestacionRespuesta modificar(@PathVariable Long id, @Valid @RequestBody PrestacionModificacion cambios) {
        return PrestacionRespuesta.de(catalogo.modificar(id, cambios.nombre(), cambios.duracionDefaultMin(),
                cambios.activa()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        catalogo.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
