package ar.edu.utn.turnos.shared.ratelimit;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

/**
 * Ayudante reutilizable de límite de tasa por clave arbitraria normalizada (por ejemplo, email en
 * operaciones públicas de reserva). Nunca registra ni expone la clave en mensajes de error (RN-35).
 */
public final class LimitadorPorClave {

    private final LimitadorTasaEnMemoria limitador;

    public LimitadorPorClave(RateLimitProperties propiedades, Clock reloj) {
        Objects.requireNonNull(propiedades, "propiedades");
        this.limitador = new LimitadorTasaEnMemoria(
                propiedades.maxSolicitudesPorClave(),
                propiedades.ventanaClave(),
                reloj);
    }

    public LimitadorTasaEnMemoria.Resultado evaluar(String clave) {
        return limitador.evaluar(normalizar(clave));
    }

    public void verificarOConsumir(String clave) {
        var resultado = evaluar(clave);
        if (!resultado.permitido()) {
            throw new LimiteTasaSuperadoException(resultado.segundosReintento());
        }
    }

    private static String normalizar(String clave) {
        Objects.requireNonNull(clave, "clave");
        return clave.trim().toLowerCase(Locale.ROOT);
    }
}
