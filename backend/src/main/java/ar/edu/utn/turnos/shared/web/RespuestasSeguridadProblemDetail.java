package ar.edu.utn.turnos.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Respuestas 401 y 403 de Spring Security en formato Problem Details. Las reutilizan C-01 y C-03
 * para que todo error de acceso tenga el mismo formato que el resto de la API.
 */
@Component
public class RespuestasSeguridadProblemDetail implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String DETALLE_NO_AUTENTICADO = "Se requiere autenticación para acceder a este recurso";
    static final String DETALLE_PROHIBIDO = "No tiene permisos para realizar esta operación";

    private final ObjectMapper objectMapper;

    public RespuestasSeguridadProblemDetail(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, DETALLE_NO_AUTENTICADO);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, DETALLE_PROHIBIDO);
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
