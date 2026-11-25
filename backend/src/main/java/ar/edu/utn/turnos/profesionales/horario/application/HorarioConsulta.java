package ar.edu.utn.turnos.profesionales.horario.application;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;

/** Puerto de agenda sobre los tramos de atención semanales (RN-04, RN-16, US-013). */
public interface HorarioConsulta {

    /** Indica si un intervalo cabe completo en un único tramo de atención. */
    boolean contiene(Long profesionalId, Intervalo intervalo);

    /** Tramos semanales del profesional, o de todos si {@code profesionalId} es {@code null} (C-28). */
    List<HorarioDatos> tramos(Long profesionalId);
}
