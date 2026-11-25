package ar.edu.utn.turnos.listaespera.application;

import java.time.LocalDate;
import java.time.LocalTime;

/** Datos del alta ya con formato válido. {@code fechaDesde} nula significa "desde hoy". */
public record DatosAlta(Long pacienteId, Long prestacionId, Long profesionalId, LocalDate fechaDesde,
        LocalDate fechaHasta, LocalTime franjaDesde, LocalTime franjaHasta) {
}
