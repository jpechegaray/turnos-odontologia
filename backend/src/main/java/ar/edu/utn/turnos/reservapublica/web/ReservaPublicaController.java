package ar.edu.utn.turnos.reservapublica.web;

import ar.edu.utn.turnos.reservapublica.application.CatalogoPublico;
import ar.edu.utn.turnos.reservapublica.application.DisponibilidadPublica;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consultas anónimas de la reserva online (sin sesión ni CSRF; con límite de tasa por IP, RN-27). */
@RestController
@RequestMapping("/api/public")
public class ReservaPublicaController {

    private final CatalogoPublico catalogo;
    private final DisponibilidadPublica disponibilidad;

    public ReservaPublicaController(CatalogoPublico catalogo, DisponibilidadPublica disponibilidad) {
        this.catalogo = catalogo;
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/prestaciones")
    public List<PrestacionPublicaDto> prestaciones() {
        return catalogo.prestacionesReservables().stream().map(PrestacionPublicaDto::de).toList();
    }

    @GetMapping("/profesionales")
    public List<ProfesionalPublicoDto> profesionales(@RequestParam Long prestacionId) {
        return catalogo.profesionalesReservables(prestacionId).stream().map(ProfesionalPublicoDto::de).toList();
    }

    @GetMapping("/disponibilidad")
    public List<HorarioPublicoDto> disponibilidad(@Valid @ModelAttribute ConsultaDisponibilidadPublica consulta) {
        return disponibilidad.consultar(consulta.profesionalId(), consulta.prestacionId(),
                        consulta.desde().toInstant(), consulta.hasta().toInstant())
                .stream().map(HorarioPublicoDto::de).toList();
    }
}
