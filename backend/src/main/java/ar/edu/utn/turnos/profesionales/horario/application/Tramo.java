package ar.edu.utn.turnos.profesionales.horario.application;

import java.time.LocalTime;

/** Día ISO y tramo semiabierto {@code [horaInicio, horaFin)} en hora local, ya validados. */
public record Tramo(int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
    public Tramo {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("El día debe estar entre 1 y 7");
        }
        if (horaInicio == null || horaFin == null || !horaFin.isAfter(horaInicio)) {
            throw new IllegalArgumentException("El fin debe ser posterior al inicio");
        }
    }
}
