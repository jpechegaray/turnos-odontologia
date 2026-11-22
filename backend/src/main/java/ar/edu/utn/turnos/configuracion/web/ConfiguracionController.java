package ar.edu.utn.turnos.configuracion.web;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.configuracion.application.ReemplazarConfiguracion;
import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionServicio servicio;
    private final ReemplazarConfiguracion reemplazar;
    private final ObjectReader lector;

    public ConfiguracionController(ConfiguracionServicio servicio, ReemplazarConfiguracion reemplazar,
            ObjectMapper json) {
        this.servicio = servicio;
        this.reemplazar = reemplazar;
        this.lector = json.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public Map<String, Object> consultar() {
        return servicio.consultar();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reemplazar(InputStream cuerpo) {
        return reemplazar.ejecutar(leerParametros(cuerpo));
    }

    private Map<ParametroConfiguracion, Object> leerParametros(InputStream cuerpo) {
        JsonNode datos;
        try {
            datos = lector.readTree(cuerpo);
        } catch (IOException ex) {
            throw new CuerpoInvalido("El cuerpo debe contener un objeto JSON válido sin claves repetidas", null);
        }
        if (datos == null || !datos.isObject()) {
            throw new CuerpoInvalido("El cuerpo debe ser un objeto JSON", null);
        }

        Map<ParametroConfiguracion, Object> valores = new EnumMap<>(ParametroConfiguracion.class);
        for (var parametro : ParametroConfiguracion.values()) {
            var nodo = datos.get(parametro.clave());
            if (nodo == null || !(parametro.esNumerico() ? nodo.isNumber() : nodo.isTextual())) {
                throw new CuerpoInvalido("Falta un parámetro obligatorio o su tipo es incorrecto", parametro.clave());
            }
            valores.put(parametro, parametro.esNumerico() ? nodo.decimalValue() : nodo.textValue());
        }
        if (datos.size() != valores.size()) {
            throw new CuerpoInvalido("El cuerpo contiene claves que no pertenecen a la configuración", null);
        }
        return valores;
    }

    @ExceptionHandler(CuerpoInvalido.class)
    ProblemDetail manejarCuerpoInvalido(CuerpoInvalido error) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
        if (error.campo != null) {
            problema.setProperty("errores", List.of(Map.of("campo", error.campo, "mensaje", error.getMessage())));
        }
        return problema;
    }

    private static final class CuerpoInvalido extends RuntimeException {

        private final String campo;

        private CuerpoInvalido(String mensaje, String campo) {
            super(mensaje);
            this.campo = campo;
        }
    }
}
