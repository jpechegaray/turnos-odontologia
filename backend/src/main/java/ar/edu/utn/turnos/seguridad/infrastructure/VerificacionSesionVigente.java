package ar.edu.utn.turnos.seguridad.infrastructure;

import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Invalida la sesión del personal si su usuario fue desactivado o cambió de rol o de email (C-13).
 * El request sigue como anónimo y el entry point de C-03 responde 401.
 *
 * <p>No es un bean a propósito: Spring Boot registraría un Filter bean también en la cadena del servlet,
 * fuera de Spring Security, y OncePerRequestFilter lo marcaría como ya ejecutado antes de tener el
 * contexto de seguridad. Lo crea SecurityConfig.
 */
final class VerificacionSesionVigente extends OncePerRequestFilter {

    private final EstadoSesionPersonal estado;

    VerificacionSesionVigente(EstadoSesionPersonal estado) {
        this.estado = estado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof IdentidadPersonal identidad
                && !estado.sigueVigente(identidad)) {
            var sesion = request.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            SecurityContextHolder.clearContext();
        }
        cadena.doFilter(request, response);
    }
}
