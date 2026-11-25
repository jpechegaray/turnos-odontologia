package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.util.List;

/**
 * Consulta interna de turnos activos ({@code PROGRAMADO} o {@code CONFIRMADO}) para validar bloqueos
 * (RN-17) y bajas lógicas de sillones, profesionales y prestaciones (RN-37), proyectando solo datos
 * operativos sin información del paciente (RN-35).
 */
public interface TurnosActivosConsulta {

    /**
     * Turnos activos del profesional que se solapan con {@code intervalo} en sentido semiabierto
     * {@code [inicio, fin)} (RN-02), ordenados por {@code inicio ASC, id ASC}.
     */
    List<TurnoEnConflicto> enIntervaloDeProfesional(Long profesionalId, Intervalo intervalo);

    /** Turnos activos del profesional con {@code fin} posterior al reloj actual, ordenados por {@code inicio ASC, id ASC}. */
    List<TurnoEnConflicto> futurosDeProfesional(Long profesionalId);

    /** Turnos activos del sillón con {@code fin} posterior al reloj actual, ordenados por {@code inicio ASC, id ASC}. */
    List<TurnoEnConflicto> futurosDeSillon(Long sillonId);

    /** Turnos activos de la prestación con {@code fin} posterior al reloj actual, ordenados por {@code inicio ASC, id ASC}. */
    List<TurnoEnConflicto> futurosDePrestacion(Long prestacionId);

    /** Proyección mínima de un turno en conflicto (sin datos del paciente, RN-35). */
    record TurnoEnConflicto(Long id, Instant inicio, Instant fin, String estado) {
    }
}
