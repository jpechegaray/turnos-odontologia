package ar.edu.utn.turnos.profesionales.horario.application;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;

/** Puerto de agenda: indica si un intervalo cabe completo en un único tramo de atención (RN-04, RN-16). */
public interface HorarioConsulta {
    boolean contiene(Long profesionalId, Intervalo intervalo);
}
