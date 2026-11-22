package ar.edu.utn.turnos.profesionales.bloqueo.web;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

record AltaBloqueo(
        @JsonDeserialize(using = AltaBloqueo.TextoEstricto.class) @NotBlank String inicio,
        @JsonDeserialize(using = AltaBloqueo.TextoEstricto.class) @NotBlank String fin,
        @JsonDeserialize(using = AltaBloqueo.TextoEstricto.class) @NotBlank @Size(max = 255) String motivo) {

    private static final Pattern DEMASIADOS_DECIMALES = Pattern.compile("\\.[0-9]{7}");

    @AssertTrue(message = "El intervalo debe tener fechas con offset y hasta seis decimales; el fin debe ser posterior")
    @JsonIgnore
    public boolean isIntervaloValido() {
        if (inicio == null || fin == null || inicio.isBlank() || fin.isBlank()) {
            return true; // Los campos faltantes los informa @NotBlank.
        }
        if (DEMASIADOS_DECIMALES.matcher(inicio).find() || DEMASIADOS_DECIMALES.matcher(fin).find()) {
            return false;
        }
        try {
            return fecha(fin).isAfter(fecha(inicio));
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    Intervalo intervalo() {
        return new Intervalo(fecha(inicio).toInstant(), fecha(fin).toInstant());
    }

    private static OffsetDateTime fecha(String valor) {
        return OffsetDateTime.parse(valor, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

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
}
