package ar.edu.utn.turnos.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro HTTP en memoria que limita las solicitudes por dirección IP sobre {@code /api/public/**}
 * (RN-27) y responde 429 Problem Details con cabecera {@code Retry-After} cuando se supera el umbral.
 */
public final class FiltroLimiteTasaPublico extends OncePerRequestFilter {

    private static final String PREFIJO_PUBLICO = "/api/public/";
    private static final String RUTA_PUBLICA_RAIZ = "/api/public";
    private static final String PROPIEDAD_CODIGO = "codigo";
    private static final String IP_DESCONOCIDA = "desconocida";

    private final LimitadorTasaEnMemoria limitadorIp;
    private final ObjectMapper objectMapper;

    public FiltroLimiteTasaPublico(LimitadorTasaEnMemoria limitadorIp, ObjectMapper objectMapper) {
        this.limitadorIp = Objects.requireNonNull(limitadorIp, "limitadorIp");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = rutaRelativa(request);
        return !ruta.startsWith(PREFIJO_PUBLICO) && !ruta.equals(RUTA_PUBLICA_RAIZ);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = extraerIp(request);
        var resultado = limitadorIp.evaluar(ip);
        if (resultado.permitido()) {
            filterChain.doFilter(request, response);
            return;
        }
        escribirRespuesta429(response, resultado.segundosReintento());
    }

    private static String rutaRelativa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static String extraerIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? IP_DESCONOCIDA : ip.trim();
    }

    private void escribirRespuesta429(HttpServletResponse response, long segundosReintento)
            throws IOException {
        HttpStatus estado = HttpStatus.TOO_MANY_REQUESTS;
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                estado, LimiteTasaSuperadoException.DETALLE_LIMITE_SUPERADO);
        problema.setTitle(estado.getReasonPhrase());
        problema.setProperty(PROPIEDAD_CODIGO, LimiteTasaSuperadoException.CODIGO_REGLA);

        response.setStatus(estado.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1L, segundosReintento)));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), problema);
    }
}
