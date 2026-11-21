package ar.edu.utn.turnos.seguridad.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/auth/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void obtenerCsrf(CsrfToken token) {
        token.getToken();
    }
}
