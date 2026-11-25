package ar.edu.utn.turnos.agenda.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Parámetros de la búsqueda: el rango se expresa con zona, la duración es opcional. */
public record ConsultaDisponibilidad(
        @NotNull @Positive Long profesionalId,
        @NotNull @Positive Long prestacionId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
        Integer duracionMin) {
}
