package ar.edu.utn.turnos.profesionales.application;

import ar.edu.utn.turnos.profesionales.horario.application.HorarioDatos;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;

/** Lectura interna sin sesión para cálculos de agenda y reserva pública. */
public interface AgendaProfesionalConsulta {
    List<HorarioDatos> horarios(Long profesionalId);
    List<Intervalo> bloqueos(Long profesionalId, Intervalo rango);
}
