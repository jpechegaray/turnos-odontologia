package ar.edu.utn.turnos.reservapublica.gestiontoken;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/turnos/{token}")
class GestionTurnoController {
    private final GestionTurnoPorToken gestion;

    GestionTurnoController(GestionTurnoPorToken gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    GestionTurnoPorToken.Detalle consultar(@PathVariable String token) {
        return gestion.consultar(token);
    }

    @PostMapping("/confirmar")
    GestionTurnoPorToken.Detalle confirmar(@PathVariable String token) {
        return gestion.confirmar(token);
    }

    @PostMapping("/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelar(@PathVariable String token) {
        gestion.cancelar(token);
    }
}
