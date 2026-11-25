package ar.edu.utn.turnos.listaespera.oferta.web;

import ar.edu.utn.turnos.listaespera.oferta.application.OfertaPublica;
import ar.edu.utn.turnos.listaespera.oferta.application.TomaDeOferta;
import ar.edu.utn.turnos.listaespera.oferta.application.TurnoTomado;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enlace público de la oferta de hueco (C-42, Flujo 7): sin sesión ni CSRF, como todo {@code /api/public/**}, y con
 * el límite de tasa por IP de C-17. El token identifica al paciente y solo da acceso a esta oferta (RN-38).
 */
@RestController
@RequestMapping("/api/public/ofertas/{token}")
class OfertaPublicaController {
    private final TomaDeOferta toma;

    OfertaPublicaController(TomaDeOferta toma) {
        this.toma = toma;
    }

    @GetMapping
    OfertaPublica consultar(@PathVariable String token) {
        return toma.consultar(token);
    }

    @PostMapping("/tomar")
    @ResponseStatus(HttpStatus.CREATED)
    TurnoTomado tomar(@PathVariable String token) {
        return toma.tomar(token);
    }
}
