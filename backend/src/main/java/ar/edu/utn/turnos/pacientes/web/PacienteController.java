package ar.edu.utn.turnos.pacientes.web;

import ar.edu.utn.turnos.pacientes.application.PacienteServicio;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteServicio servicio;

    public PacienteController(@Autowired(required = false) PacienteServicio servicio) {
        this.servicio = servicio;
    }

    private PacienteResponse toResponse(Paciente p) {
        PacienteResponse r = new PacienteResponse();
        r.setId(p.id());
        r.setNombre(p.nombre());
        r.setApellido(p.apellido());
        r.setDni(p.dni());
        r.setEmail(p.email());
        r.setTelefono(p.telefono());
        r.setObraSocial(p.obraSocial());
        r.setActivo(p.activo());
        r.setCreadoEn(p.creadoEn());
        r.setActualizadoEn(null);
        return r;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPCION','ADMIN')")
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        Paciente dominio = Paciente.nuevo(
                request.getNombre(),
                request.getApellido(),
                request.getDni(),
                request.getEmail(),
                request.getTelefono(),
                request.getObraSocial(),
                null,
                null);
        Paciente creado = servicio.crear(dominio);
        return ResponseEntity.status(201).body(toResponse(creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCION','ADMIN')")
    public ResponseEntity<PacienteResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody PacienteRequest request) {
        Paciente nuevosDatos = Paciente.nuevo(
                request.getNombre(),
                request.getApellido(),
                request.getDni(),
                request.getEmail(),
                request.getTelefono(),
                request.getObraSocial(),
                null,
                null);
        Paciente actualizado = servicio.actualizar(id, nuevosDatos);
        return ResponseEntity.ok(toResponse(actualizado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPCION','ADMIN')")
    public ResponseEntity<PacienteResponse> obtener(@PathVariable Long id) {
        Paciente p = servicio.obtener(id);
        return ResponseEntity.ok(toResponse(p));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPCION','ADMIN')")
    public ResponseEntity<List<PacienteResponse>> buscar(
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String email) {
        List<Paciente> lista = servicio.buscar(apellido, nombre, dni, email);
        List<PacienteResponse> resp = lista.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
