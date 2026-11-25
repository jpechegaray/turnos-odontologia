package ar.edu.utn.turnos.agenda.web;

import ar.edu.utn.turnos.agenda.application.ConsultaAgenda;
import ar.edu.utn.turnos.agenda.domain.VistaAgenda;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Agenda diaria y semanal (C-28, US-013). Una vista o una fecha inválidas responden 400 en Problem Details. */
@RestController
@RequestMapping("/api/agenda")
class AgendaController {
    private final ConsultaAgenda consulta;

    AgendaController(ConsultaAgenda consulta) {
        this.consulta = consulta;
    }

    @GetMapping
    RespuestaAgenda consultar(
            @RequestParam(defaultValue = "dia") String vista,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId) {
        return RespuestaAgenda.de(consulta.consultar(vistaDe(vista), fecha, profesionalId, sillonId));
    }

    private static VistaAgenda vistaDe(String texto) {
        try {
            return VistaAgenda.desdeTexto(texto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La vista debe ser dia o semana");
        }
    }
}
