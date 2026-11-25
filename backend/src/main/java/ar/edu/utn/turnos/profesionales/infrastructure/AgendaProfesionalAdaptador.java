package ar.edu.utn.turnos.profesionales.infrastructure;

import ar.edu.utn.turnos.profesionales.application.AgendaProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.horario.application.HorarioDatos;
import ar.edu.utn.turnos.profesionales.horario.infrastructure.HorariosPersistencia;
import ar.edu.utn.turnos.profesionales.bloqueo.infrastructure.BloqueosPersistencia;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Los detalles de persistencia permanecen dentro del módulo profesionales. */
@Component
class AgendaProfesionalAdaptador implements AgendaProfesionalConsulta {
    private final HorariosPersistencia horarios;
    private final BloqueosPersistencia bloqueos;

    AgendaProfesionalAdaptador(HorariosPersistencia horarios, BloqueosPersistencia bloqueos) {
        this.horarios = horarios;
        this.bloqueos = bloqueos;
    }

    @Override
    public List<HorarioDatos> horarios(Long profesionalId) {
        return horarios.listar(profesionalId);
    }

    @Override
    public List<Intervalo> bloqueos(Long profesionalId, Intervalo rango) {
        return bloqueos.listar(profesionalId).stream().map(b -> new Intervalo(b.inicio(), b.fin()))
                .filter(b -> b.solapaCon(rango)).toList();
    }
}
