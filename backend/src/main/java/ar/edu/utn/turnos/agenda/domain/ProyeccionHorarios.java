package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Lleva los tramos semanales de atención a cada fecha del período, en hora de Buenos Aires (US-013). */
public final class ProyeccionHorarios {

    /** Tramo semanal {@code [horaInicio, horaFin)} de un profesional; {@code diaSemana} es ISO (1 = lunes). */
    public record TramoSemanal(Long profesionalId, int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
    }

    /** Un tramo en una fecha concreta, ya convertido a instantes. */
    public record HorarioDelDia(Long profesionalId, LocalDate fecha, Instant inicio, Instant fin) {
    }

    private static final Comparator<HorarioDelDia> ORDEN = Comparator.comparing(HorarioDelDia::fecha)
            .thenComparing(HorarioDelDia::profesionalId)
            .thenComparing(HorarioDelDia::inicio);

    private ProyeccionHorarios() {
    }

    public static List<HorarioDelDia> proyectar(List<TramoSemanal> tramos, PeriodoAgenda periodo) {
        return periodo.fechas().stream()
                .flatMap(fecha -> tramos.stream()
                        .filter(tramo -> tramo.diaSemana() == fecha.getDayOfWeek().getValue())
                        .map(tramo -> new HorarioDelDia(tramo.profesionalId(), fecha,
                                fecha.atTime(tramo.horaInicio()).atZone(ZonaHoraria.BUENOS_AIRES).toInstant(),
                                fecha.atTime(tramo.horaFin()).atZone(ZonaHoraria.BUENOS_AIRES).toInstant())))
                .sorted(ORDEN)
                .toList();
    }
}
