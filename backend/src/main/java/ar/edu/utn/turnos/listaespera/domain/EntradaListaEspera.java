package ar.edu.utn.turnos.listaespera.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Entrada de la lista de espera (RN-21). Inmutable: cada transición devuelve una entrada nueva y solo
 * se permite desde {@link EstadoListaEspera#ACTIVA}. Las fechas y la franja son de pared, en Buenos Aires.
 */
public record EntradaListaEspera(
        Long id,
        Long pacienteId,
        Long prestacionId,
        Long profesionalId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalTime franjaDesde,
        LocalTime franjaHasta,
        EstadoListaEspera estado,
        Long turnoAsignadoId,
        Instant creadoEn) {

    public EntradaListaEspera {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(fechaDesde, "fechaDesde");
        Objects.requireNonNull(fechaHasta, "fechaHasta");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (fechaHasta.isBefore(fechaDesde)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if ((franjaDesde == null) != (franjaHasta == null)) {
            throw new IllegalArgumentException("La franja lleva las dos horas o ninguna");
        }
        if (franjaDesde != null && !franjaHasta.isAfter(franjaDesde)) {
            throw new IllegalArgumentException("El fin de la franja debe ser posterior al inicio");
        }
        if ((estado == EstadoListaEspera.ASIGNADA) != (turnoAsignadoId != null)) {
            throw new IllegalArgumentException("Solo una entrada ASIGNADA tiene turno asignado");
        }
    }

    public static EntradaListaEspera nueva(Long pacienteId, Long prestacionId, Long profesionalId,
            LocalDate fechaDesde, LocalDate fechaHasta, LocalTime franjaDesde, LocalTime franjaHasta,
            Instant creadoEn) {
        return new EntradaListaEspera(null, pacienteId, prestacionId, profesionalId, fechaDesde, fechaHasta,
                franjaDesde, franjaHasta, EstadoListaEspera.ACTIVA, null, creadoEn);
    }

    /** La entrada consiguió turno (RN-24): la usarán la oferta de huecos y la asignación manual. */
    public EntradaListaEspera asignar(Long turnoId) {
        if (turnoId == null) {
            throw new IllegalArgumentException("Asignar requiere el turno");
        }
        return pasarA(EstadoListaEspera.ASIGNADA, turnoId);
    }

    public EntradaListaEspera cancelar() {
        return pasarA(EstadoListaEspera.CANCELADA, null);
    }

    public EntradaListaEspera vencer() {
        return pasarA(EstadoListaEspera.VENCIDA, null);
    }

    private EntradaListaEspera pasarA(EstadoListaEspera destino, Long turnoId) {
        if (estado != EstadoListaEspera.ACTIVA) {
            throw new ReglaNegocioException("ENTRADA_NO_ACTIVA", EstadoError.CONFLICTO_409,
                    "La entrada de la lista de espera ya no está activa");
        }
        return new EntradaListaEspera(id, pacienteId, prestacionId, profesionalId, fechaDesde, fechaHasta,
                franjaDesde, franjaHasta, destino, turnoId, creadoEn);
    }
}
