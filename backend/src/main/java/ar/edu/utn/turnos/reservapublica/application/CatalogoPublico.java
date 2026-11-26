package ar.edu.utn.turnos.reservapublica.application;

import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Catálogo reservable online (Flujo 4, paso 1): solo lo activo y asociado a algo activo. Hace una
 * cantidad fija de consultas por pedido (sin N+1).
 */
@Service
public class CatalogoPublico {

    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalConsulta profesionales;
    private final ProfesionalRealizaPrestacion asignaciones;

    public CatalogoPublico(ConsultaPrestaciones prestaciones, ProfesionalConsulta profesionales,
            ProfesionalRealizaPrestacion asignaciones) {
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.asignaciones = asignaciones;
    }

    /** @return las prestaciones activas que realiza al menos un profesional activo */
    public List<PrestacionResumen> prestacionesReservables() {
        var activos = profesionales.listarActivos().stream().map(ProfesionalConsulta.Datos::id).toList();
        var conProfesional = asignaciones.prestacionesConProfesional(activos);
        if (conProfesional.isEmpty()) {
            return List.of();
        }
        return prestaciones.listarActivas().stream().filter(p -> conProfesional.contains(p.id())).toList();
    }

    /** @return los profesionales activos que realizan la prestación; vacío si no es reservable */
    public List<ProfesionalConsulta.Datos> profesionalesReservables(Long prestacionId) {
        boolean activa = prestaciones.buscarPorId(prestacionId).map(PrestacionResumen::activa).orElse(false);
        if (!activa) {
            return List.of();
        }
        var asociados = asignaciones.profesionalesQueRealizan(prestacionId);
        if (asociados.isEmpty()) {
            return List.of();
        }
        return profesionales.listarActivos().stream().filter(p -> asociados.contains(p.id())).toList();
    }
}
