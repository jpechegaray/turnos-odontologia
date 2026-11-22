package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.infrastructure.GestionUsuarios;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Gestión de usuarios del personal (US-002). Solo ADMIN: lo exigen la regla por ruta y el servicio. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    private final GestionUsuarios gestion;

    public UsuariosController(GestionUsuarios gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    List<UsuarioRespuesta> listar() {
        return gestion.listar().stream().map(UsuarioRespuesta::de).toList();
    }

    @GetMapping("/{id}")
    UsuarioRespuesta obtener(@PathVariable Long id) {
        return UsuarioRespuesta.de(gestion.obtener(id));
    }

    @PostMapping
    ResponseEntity<UsuarioRespuesta> crear(@Valid @RequestBody UsuarioAlta alta) {
        var creado = UsuarioRespuesta.de(gestion.crear(alta.email(), alta.nombre(), alta.rol(), alta.password(),
                alta.profesionalId()));
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    UsuarioRespuesta modificar(@PathVariable Long id, @Valid @RequestBody UsuarioModificacion cambios) {
        return UsuarioRespuesta.de(gestion.modificar(id, cambios.email(), cambios.nombre(), cambios.rol(),
                cambios.profesionalId(), cambios.activo()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> desactivar(@PathVariable Long id) {
        gestion.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
