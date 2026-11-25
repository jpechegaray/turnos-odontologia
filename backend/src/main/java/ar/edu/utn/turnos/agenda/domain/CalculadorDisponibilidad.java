package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Cálculo de sugerencias sobre una instantánea de la agenda. */
public final class CalculadorDisponibilidad {
    public List<HorarioDisponible> calcular(Long profesionalId, Intervalo rango, int duracionMin, Instant ahora,
            List<Intervalo> horarios, List<Intervalo> bloqueos, List<TurnoOcupado> turnos, List<Long> sillones) {
        new DuracionPrestacion(duracionMin);
        var resultado = new ArrayList<HorarioDisponible>();
        var ordenados = sillones.stream().distinct().sorted().toList();
        if (ordenados.isEmpty() || !rango.fin().isAfter(ahora)) {
            return List.of();
        }
        var ocupados = turnos.stream().filter(t -> t.estado().ocupaLugar()).toList();
        var inferior = rango.inicio().isAfter(ahora) ? rango.inicio() : ahora;
        var inicio = proximaGrilla(inferior);
        if (!inicio.isAfter(ahora)) {
            inicio = inicio.plus(5, ChronoUnit.MINUTES);
        }
        for (; !inicio.plus(duracionMin, ChronoUnit.MINUTES).isAfter(rango.fin());
                inicio = inicio.plus(5, ChronoUnit.MINUTES)) {
            var candidato = Intervalo.desde(inicio, duracionMin);
            if (!mismoDia(candidato) || horarios.stream().noneMatch(h -> h.contiene(candidato))
                    || bloqueos.stream().anyMatch(b -> b.solapaCon(candidato))
                    || ocupados.stream().anyMatch(t -> t.profesionalId().equals(profesionalId)
                            && t.intervalo().solapaCon(candidato))) {
                continue;
            }
            ordenados.stream().filter(s -> ocupados.stream().noneMatch(t -> t.sillonId().equals(s)
                    && t.intervalo().solapaCon(candidato))).findFirst()
                    .ifPresent(s -> resultado.add(new HorarioDisponible(candidato.inicio(), candidato.fin(), s)));
        }
        return List.copyOf(resultado);
    }

    private static Instant proximaGrilla(Instant instante) {
        var local = instante.atZone(ZonaHoraria.BUENOS_AIRES);
        var piso = local.withMinute(local.getMinute() / 5 * 5).withSecond(0).withNano(0);
        return (piso.toInstant().isBefore(instante) ? piso.plusMinutes(5) : piso).toInstant();
    }

    private static boolean mismoDia(Intervalo intervalo) {
        return intervalo.inicio().atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate()
                .equals(intervalo.fin().atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate());
    }
}
