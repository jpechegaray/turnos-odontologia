package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Período que muestra la agenda, con fechas locales inclusive y su intervalo {@code [inicio, fin)} en
 * hora de Buenos Aires (RN-14). Se calcula con {@code ZoneId}, nunca con un desfase fijo.
 */
public record PeriodoAgenda(VistaAgenda vista, LocalDate desde, LocalDate hasta) {

    public PeriodoAgenda {
        if (vista == null || desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Período de agenda inválido");
        }
    }

    /** El día de {@code fecha}, o la semana de lunes a domingo que la contiene. */
    public static PeriodoAgenda de(VistaAgenda vista, LocalDate fecha) {
        if (vista == VistaAgenda.SEMANA) {
            LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new PeriodoAgenda(vista, lunes, lunes.plusDays(6));
        }
        return new PeriodoAgenda(vista, fecha, fecha);
    }

    /** Desde las 00:00 de {@code desde} hasta las 00:00 del día siguiente a {@code hasta}, en Buenos Aires. */
    public Intervalo intervalo() {
        return new Intervalo(desde.atStartOfDay(ZonaHoraria.BUENOS_AIRES).toInstant(),
                hasta.plusDays(1).atStartOfDay(ZonaHoraria.BUENOS_AIRES).toInstant());
    }

    public List<LocalDate> fechas() {
        return desde.datesUntil(hasta.plusDays(1)).toList();
    }
}
