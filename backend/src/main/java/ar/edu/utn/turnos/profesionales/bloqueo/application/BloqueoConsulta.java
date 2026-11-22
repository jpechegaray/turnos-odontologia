package ar.edu.utn.turnos.profesionales.bloqueo.application;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;

public interface BloqueoConsulta {
    boolean solapa(Long profesionalId, Intervalo intervalo);
}
