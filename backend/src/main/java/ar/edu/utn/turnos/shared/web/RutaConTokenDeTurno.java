package ar.edu.utn.turnos.shared.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Rutas públicas cuyo segmento lleva una credencial: el enlace del turno (C-32, C-39) y el de la oferta de hueco
 * (C-42). Da la referencia pública sin ese segmento, para usarla en los errores.
 */
final class RutaConTokenDeTurno {
    private static final List<String> BASES = List.of("/api/public/turnos", "/api/public/ofertas");
    private static final String ATRIBUTO = RutaConTokenDeTurno.class.getName();

    private RutaConTokenDeTurno() {
    }

    static boolean coincide(HttpServletRequest request) {
        return base(request).isPresent();
    }

    /** Referencia segura del error: la base de la ruta, sin el token. */
    static URI referencia(HttpServletRequest request) {
        return URI.create(base(request).orElse(BASES.get(0)));
    }

    static void marcar(HttpServletRequest request) {
        base(request).ifPresent(base -> request.setAttribute(ATRIBUTO, base));
    }

    private static Optional<String> base(HttpServletRequest request) {
        if (request.getAttribute(ATRIBUTO) instanceof String marcada) {
            return Optional.of(marcada);
        }
        return BASES.stream().filter(base -> bajo(request, base)).findFirst();
    }

    private static boolean bajo(HttpServletRequest request, String base) {
        String prefijo = request.getContextPath() + base + "/";
        return bajoPrefijo(request.getRequestURI(), prefijo)
                || bajoPrefijo(request.getServletPath(), base + "/")
                || bajoPrefijo(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), prefijo);
    }

    private static boolean bajoPrefijo(Object ruta, String prefijo) {
        return ruta instanceof String valor && valor.startsWith(prefijo);
    }
}
