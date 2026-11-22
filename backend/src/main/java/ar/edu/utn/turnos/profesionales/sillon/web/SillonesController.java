package ar.edu.utn.turnos.profesionales.sillon.web;

import ar.edu.utn.turnos.profesionales.sillon.application.ServicioSillones;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta.Datos;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sillones")
class SillonesController {

    private final ServicioSillones servicio;

    SillonesController(ServicioSillones servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    List<SillonRespuesta> listar() {
        return servicio.listar().stream().map(SillonRespuesta::desde).toList();
    }

    @GetMapping("/{id}")
    SillonRespuesta detalle(@PathVariable Long id) {
        return SillonRespuesta.desde(servicio.detalle(id));
    }

    @PostMapping
    ResponseEntity<SillonRespuesta> crear(@Valid @RequestBody AltaSillon solicitud) {
        var respuesta = SillonRespuesta.desde(servicio.crear(solicitud.nombre(), solicitud.descripcion()));
        return ResponseEntity.created(URI.create("/api/sillones/" + respuesta.id())).body(respuesta);
    }

    @PutMapping("/{id}")
    SillonRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ActualizacionSillon solicitud) {
        return SillonRespuesta.desde(servicio.actualizar(id, solicitud.nombre(), solicitud.descripcion(), solicitud.activo()));
    }

    record AltaSillon(@NotBlank String nombre, String descripcion) {
    }

    record ActualizacionSillon(@NotBlank String nombre, String descripcion,
            @NotNull @JsonDeserialize(using = BooleanoEstricto.class) Boolean activo) {
    }

    static final class BooleanoEstricto extends JsonDeserializer<Boolean> {

        @Override
        public Boolean deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            return switch (json.currentToken()) {
                case VALUE_TRUE -> true;
                case VALUE_FALSE -> false;
                default -> {
                    // Deja que Bean Validation informe el campo inválido en el Problem Details.
                    json.skipChildren();
                    yield null;
                }
            };
        }
    }

    record SillonRespuesta(Long id, String nombre, String descripcion, boolean activo) {
        static SillonRespuesta desde(Datos datos) {
            return new SillonRespuesta(datos.id(), datos.nombre(), datos.descripcion(), datos.activo());
        }
    }
}
