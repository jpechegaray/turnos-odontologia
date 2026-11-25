package ar.edu.utn.turnos.profesionales.bloqueo.application;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;

/** Puerto de agenda sobre los bloqueos de un profesional (RN-04, US-013). */
public interface BloqueoConsulta {

    boolean solapa(Long profesionalId, Intervalo intervalo);

    /**
     * Bloqueos que comparten algún instante con {@code [inicio, fin)}, del profesional o de todos si
     * {@code profesionalId} es {@code null}, ordenados por inicio (C-28).
     */
    List<BloqueoDatos> queSolapan(Long profesionalId, Intervalo intervalo);
}
