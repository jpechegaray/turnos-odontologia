package ar.edu.utn.turnos.profesionales.profesional.web;

import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta.Datos;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/profesionales")
class ProfesionalesController {

    private final ServicioProfesionales servicio;

    ProfesionalesController(ServicioProfesionales servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    ResponseEntity<Datos> crear(@Valid @RequestBody AltaProfesional solicitud) {
        var datos = servicio.crear(solicitud.nombre(), solicitud.apellido(), solicitud.matricula(),
                solicitud.especialidad());
        return ResponseEntity.created(URI.create("/api/profesionales/" + datos.id())).body(datos);
    }

    @PutMapping("/{id}")
    Datos actualizar(@PathVariable Long id, @Valid @RequestBody ActualizacionProfesional solicitud) {
        return servicio.actualizar(id, solicitud.nombre(), solicitud.apellido(), solicitud.matricula(),
                solicitud.especialidad(), solicitud.activo());
    }

    @GetMapping("/{id}")
    Datos detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @GetMapping
    List<Datos> listar() {
        return servicio.listar();
    }

    record AltaProfesional(
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String nombre,
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String apellido,
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String matricula,
            @Size(max = 255) String especialidad) {
    }

    record ActualizacionProfesional(
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String nombre,
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String apellido,
            @JsonDeserialize(using = TextoEstricto.class) @NotBlank @Size(max = 255) String matricula,
            @Size(max = 255) String especialidad,
            @JsonDeserialize(using = BooleanoEstricto.class) @NotNull Boolean activo) {
    }

    // Los tipos inválidos se dejan en null para que Bean Validation informe el nombre del campo.
    static final class TextoEstricto extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            if (json.hasToken(JsonToken.VALUE_STRING)) {
                return json.getText();
            }
            json.skipChildren();
            return null;
        }
    }

    static final class BooleanoEstricto extends JsonDeserializer<Boolean> {

        @Override
        public Boolean deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            return switch (json.currentToken()) {
                case VALUE_TRUE -> true;
                case VALUE_FALSE -> false;
                default -> {
                    json.skipChildren();
                    yield null;
                }
            };
        }
    }
}
