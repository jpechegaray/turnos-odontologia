package ar.edu.utn.turnos.shared.web;

import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Formato uniforme de error de la API: Problem Details (RFC 7807) con el código de la regla. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    static final String PROPIEDAD_CODIGO = "codigo";
    static final String PROPIEDAD_ERRORES = "errores";
    static final String DETALLE_GENERICO = "Ocurrió un error inesperado";

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail manejarRegla(ReglaNegocioException ex, HttpServletRequest request) {
        HttpStatus estado = HttpStatus.valueOf(ex.estado().codigoHttp());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
        problema.setTitle(estado.getReasonPhrase());
        problema.setProperty(PROPIEDAD_CODIGO, ex.codigo());
        ex.propiedades().forEach(problema::setProperty);
        if (RutaConTokenDeTurno.coincide(request)) {
            problema.setInstance(RutaConTokenDeTurno.REFERENCIA);
        }
        return problema;
    }

    /** Agrega la lista de campos inválidos (sin el valor recibido) al 400 de Bean Validation. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("campo", error.getField(), "mensaje", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problema = ex.getBody();
        problema.setProperty(PROPIEDAD_ERRORES, errores);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (request instanceof ServletWebRequest servlet && RutaConTokenDeTurno.coincide(servlet.getRequest())) {
            // Los mensajes MVC por defecto pueden repetir la ruta solicitada (por ejemplo, si no hay un recurso).
            var seguro = ProblemDetail.forStatus(status);
            seguro.setInstance(RutaConTokenDeTurno.REFERENCIA);
            body = seguro;
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Último recurso: 500 genérico. Las excepciones de seguridad se relanzan a Spring Security (401/403).
     * En rutas con credenciales, no se registra el mensaje ni la traza de la excepción.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarInesperado(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, DETALLE_GENERICO);
        problema.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        if (RutaConTokenDeTurno.coincide(request)) {
            LOG.error("Error inesperado en un enlace de turno ({})", ex.getClass().getSimpleName());
            problema.setInstance(RutaConTokenDeTurno.REFERENCIA);
        } else {
            LOG.error("Error inesperado", ex);
        }
        return problema;
    }
}
