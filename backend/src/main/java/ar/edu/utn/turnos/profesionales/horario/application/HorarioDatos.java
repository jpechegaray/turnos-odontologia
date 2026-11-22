package ar.edu.utn.turnos.profesionales.horario.application;

import java.time.LocalTime;

public record HorarioDatos(Long id, Long profesionalId, int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
}
