package ar.edu.utn.turnos.profesionales.horario.domain;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Un intervalo candidato expresado en la hora de pared de Buenos Aires (RN-14): día ISO (1 = lunes)
 * y horas locales de inicio y fin. Solo existe si ambos extremos caen en la misma fecha local, porque
 * un turno no puede cruzar la medianoche (RN-16).
 */
public record FranjaLocal(int diaSemana, LocalTime inicio, LocalTime fin) {

    public static Optional<FranjaLocal> de(Intervalo intervalo) {
        var inicio = intervalo.inicio().atZone(ZonaHoraria.BUENOS_AIRES);
        var fin = intervalo.fin().atZone(ZonaHoraria.BUENOS_AIRES);
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            return Optional.empty();
        }
        return Optional.of(new FranjaLocal(inicio.getDayOfWeek().getValue(), inicio.toLocalTime(), fin.toLocalTime()));
    }
}
