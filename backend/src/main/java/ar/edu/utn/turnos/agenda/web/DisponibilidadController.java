package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.ConsultarDisponibilidadPersonal;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DisponibilidadController {
    private final ConsultarDisponibilidadPersonal disponibilidad;

    public DisponibilidadController(ConsultarDisponibilidadPersonal disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/api/agenda/disponibilidad")
    public List<HorarioDisponible> consultar(@Valid @ModelAttribute ConsultaDisponibilidad consulta) {
        return disponibilidad.consultar(consulta.profesionalId(), consulta.prestacionId(),
                consulta.desde().toInstant(), consulta.hasta().toInstant(), consulta.duracionMin());
    }
}
