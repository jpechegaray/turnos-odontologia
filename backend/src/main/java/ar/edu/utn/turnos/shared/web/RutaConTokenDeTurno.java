package ar.edu.utn.turnos.shared.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

/** Referencia pública sin el segmento que contiene la credencial. */
final class RutaConTokenDeTurno {
    private static final String BASE = "/api/public/turnos";
    private static final String ATRIBUTO = RutaConTokenDeTurno.class.getName();
    static final URI REFERENCIA = URI.create(BASE);

    private RutaConTokenDeTurno() {
    }

    static boolean coincide(HttpServletRequest request) {
        String prefijo = request.getContextPath() + BASE + "/";
        return Boolean.TRUE.equals(request.getAttribute(ATRIBUTO))
                || bajoPrefijo(request.getRequestURI(), prefijo)
                || bajoPrefijo(request.getServletPath(), BASE + "/")
                || bajoPrefijo(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI), prefijo);
    }

    static void marcar(HttpServletRequest request) {
        request.setAttribute(ATRIBUTO, true);
    }

    private static boolean bajoPrefijo(Object ruta, String prefijo) {
        return ruta instanceof String valor && valor.startsWith(prefijo);
    }
}
