package ar.edu.utn.turnos.reservapublica.web;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;

/** Horario libre {@code [inicio, fin)}; sin sillón ni datos de otros pacientes. */
public record HorarioPublicoDto(Instant inicio, Instant fin) {

    static HorarioPublicoDto de(Intervalo intervalo) {
        return new HorarioPublicoDto(intervalo.inicio(), intervalo.fin());
    }
}
