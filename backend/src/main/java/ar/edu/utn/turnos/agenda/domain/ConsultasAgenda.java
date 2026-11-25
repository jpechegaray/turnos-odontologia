package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;

/**
 * Lo que el validador necesita preguntar sobre la agenda. Es una interfaz del dominio: la implementa la
 * capa de aplicación con los puertos de horarios, bloqueos y turnos. El validador la llama solo cuando
 * las reglas anteriores pasaron, porque el intervalo depende de la duración de la prestación.
 */
public interface ConsultasAgenda {

    /** RN-04 y RN-16: el intervalo cabe completo en un único tramo del profesional. */
    boolean dentroDeHorario(Long profesionalId, Intervalo intervalo);

    /** RN-04: el intervalo toca algún bloqueo del profesional. */
    boolean enBloqueo(Long profesionalId, Intervalo intervalo);

    /** Turnos del profesional o del sillón que solapan con el intervalo (RN-01). */
    List<TurnoOcupado> turnosQueSolapan(Long profesionalId, Long sillonId, Intervalo intervalo);
}
