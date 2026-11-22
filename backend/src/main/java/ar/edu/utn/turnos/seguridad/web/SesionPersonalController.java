package ar.edu.utn.turnos.seguridad.web;

import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.infrastructure.AutenticacionPersonal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SesionPersonalController {

    private final AutenticacionPersonal autenticacion;
    private final SessionAuthenticationStrategy estrategiaSesion;
    private final SecurityContextRepository contextos;

    public SesionPersonalController(AutenticacionPersonal autenticacion,
            SessionAuthenticationStrategy estrategiaSesion, SecurityContextRepository contextos) {
        this.autenticacion = autenticacion;
        this.estrategiaSesion = estrategiaSesion;
        this.contextos = contextos;
    }

    @PostMapping("/login")
    ResponseEntity<?> ingresar(@Valid @RequestBody Credenciales credenciales,
            HttpServletRequest request, HttpServletResponse response) {
        var identidad = autenticacion.autenticar(credenciales.email(), credenciales.password());
        if (identidad.isEmpty()) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                    "No pudimos iniciar sesión. Revisá tus credenciales o volvé a intentar más tarde.")).build();
        }
        var personal = identidad.orElseThrow();
        var usuario = UsernamePasswordAuthenticationToken.authenticated(personal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + personal.rol().name())));
        estrategiaSesion.onAuthentication(usuario, request, response);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(usuario);
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, request, response);
        return ResponseEntity.ok(personal);
    }

    @GetMapping("/me")
    IdentidadPersonal consultar(@AuthenticationPrincipal IdentidadPersonal identidad) {
        return identidad;
    }

    record Credenciales(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 256) String password) {
        @Override
        public String toString() {
            return "Credenciales[redactadas]";
        }
    }
}
