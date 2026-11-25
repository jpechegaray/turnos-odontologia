package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Frontera de autorización del caso de uso del personal. */
@Service
public class ConsultarDisponibilidadPersonal {
    private final DisponibilidadService disponibilidad;

    public ConsultarDisponibilidadPersonal(DisponibilidadService disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    public List<HorarioDisponible> consultar(Long profesionalId, Long prestacionId, Instant desde, Instant hasta,
            Integer duracionMin) {
        return disponibilidad.consultar(profesionalId, prestacionId, desde, hasta, duracionMin);
    }
}
