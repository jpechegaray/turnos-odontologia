package ar.edu.utn.turnos.listaespera.web;

import ar.edu.utn.turnos.listaespera.application.DatosAlta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Cuerpo del alta. Lee textos y enteros sin coerción para que un tipo raro devuelva 400. */
record SolicitudAlta(
        @JsonDeserialize(using = SolicitudAlta.EnteroEstricto.class) @NotNull Long pacienteId,
        @JsonDeserialize(using = SolicitudAlta.EnteroEstricto.class) @NotNull Long prestacionId,
        @JsonDeserialize(using = SolicitudAlta.EnteroEstricto.class) Long profesionalId,
        @JsonDeserialize(using = SolicitudAlta.TextoEstricto.class) @Pattern(regexp = FECHA) String fechaDesde,
        @JsonDeserialize(using = SolicitudAlta.TextoEstricto.class) @NotNull @Pattern(regexp = FECHA) String fechaHasta,
        @JsonDeserialize(using = SolicitudAlta.TextoEstricto.class) @Pattern(regexp = HORA) String franjaDesde,
        @JsonDeserialize(using = SolicitudAlta.TextoEstricto.class) @Pattern(regexp = HORA) String franjaHasta) {

    static final String FECHA = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
    static final String HORA = "([01][0-9]|2[0-3]):[0-5][0-9]";

    @AssertTrue(message = "Las fechas deben existir y la de fin no puede ser anterior a la de inicio")
    @JsonIgnore
    public boolean isFechasValidas() {
        if (!fechaReal(fechaHasta) || !fechaReal(fechaDesde)) {
            return false;
        }
        if (!esFecha(fechaHasta) || !esFecha(fechaDesde)) {
            return true; // Faltantes y formatos los informan @NotNull y @Pattern.
        }
        return !LocalDate.parse(fechaHasta).isBefore(LocalDate.parse(fechaDesde));
    }

    @AssertTrue(message = "La franja lleva las dos horas o ninguna, y el fin debe ser posterior al inicio")
    @JsonIgnore
    public boolean isFranjaValida() {
        if (franjaDesde == null && franjaHasta == null) {
            return true;
        }
        if (franjaDesde == null || franjaHasta == null) {
            return false;
        }
        if (!franjaDesde.matches(HORA) || !franjaHasta.matches(HORA)) {
            return true; // El formato lo informa @Pattern.
        }
        return LocalTime.parse(franjaHasta).isAfter(LocalTime.parse(franjaDesde));
    }

    DatosAlta datos() {
        return new DatosAlta(pacienteId, prestacionId, profesionalId,
                fechaDesde == null ? null : LocalDate.parse(fechaDesde), LocalDate.parse(fechaHasta),
                franjaDesde == null ? null : LocalTime.parse(franjaDesde),
                franjaHasta == null ? null : LocalTime.parse(franjaHasta));
    }

    /** Falso solo si el texto respeta el patrón pero no es una fecha del calendario (2026-02-30). */
    private static boolean fechaReal(String valor) {
        if (valor == null || !valor.matches(FECHA)) {
            return true;
        }
        try {
            LocalDate.parse(valor);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean esFecha(String valor) {
        return valor != null && valor.matches(FECHA);
    }

    static final class TextoEstricto extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            if (json.hasToken(JsonToken.VALUE_STRING)) {
                return json.getText();
            }
            json.skipChildren();
            return "";
        }
    }

    static final class EnteroEstricto extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser json, DeserializationContext contexto) throws IOException {
            if (json.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return json.getLongValue();
            }
            throw contexto.weirdStringException(json.getText(), Long.class, "Se esperaba un entero");
        }
    }
}
