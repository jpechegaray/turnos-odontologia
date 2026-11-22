package ar.edu.utn.turnos.profesionales.horario.web;

import ar.edu.utn.turnos.profesionales.horario.application.Tramo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.time.LocalTime;

/** Cuerpo del alta y de la modificación de un tramo. Rechaza coerciones de tipo para devolver 400. */
record SolicitudTramo(
        @JsonDeserialize(using = SolicitudTramo.EnteroEstricto.class) @NotNull @Min(1) @Max(7) Integer diaSemana,
        @JsonDeserialize(using = SolicitudTramo.TextoEstricto.class) @NotNull @Pattern(regexp = HORA) String horaInicio,
        @JsonDeserialize(using = SolicitudTramo.TextoEstricto.class) @NotNull @Pattern(regexp = HORA) String horaFin) {

    /** {@code HH:mm} de 00:00 a 23:59, sin segundos. */
    static final String HORA = "([01][0-9]|2[0-3]):[0-5][0-9]";

    @AssertTrue(message = "La hora de fin debe ser posterior a la de inicio")
    @JsonIgnore
    public boolean isTramoOrdenado() {
        if (!esHora(horaInicio) || !esHora(horaFin)) {
            return true; // Los formatos inválidos los informan @NotNull y @Pattern.
        }
        return LocalTime.parse(horaFin).isAfter(LocalTime.parse(horaInicio));
    }

    Tramo tramo() {
        return new Tramo(diaSemana, LocalTime.parse(horaInicio), LocalTime.parse(horaFin));
    }

    private static boolean esHora(String valor) {
        return valor != null && valor.matches(HORA);
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

    static final class EnteroEstricto extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            if (json.hasToken(JsonToken.VALUE_NUMBER_INT) && json.getNumberType() == JsonParser.NumberType.INT) {
                return json.getIntValue();
            }
            json.skipChildren();
            return null;
        }
    }
}
