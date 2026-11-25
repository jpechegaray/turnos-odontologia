package ar.edu.utn.turnos.reservapublica.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Parámetros de la disponibilidad pública: rango con zona y sin duración (se usa la de la prestación). */
public record ConsultaDisponibilidadPublica(
        @NotNull @Positive Long profesionalId,
        @NotNull @Positive Long prestacionId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
}
