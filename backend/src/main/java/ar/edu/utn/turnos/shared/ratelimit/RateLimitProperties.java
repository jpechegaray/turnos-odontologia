package ar.edu.utn.turnos.shared.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Umbrales y ventanas del límite de tasa para rutas y claves públicas ({@code app.ratelimit}, RM-02).
 */
@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(
        int maxSolicitudesPorIp,
        Duration ventanaIp,
        int maxSolicitudesPorClave,
        Duration ventanaClave) {

    public static final int DEFAULT_MAX_SOLICITUDES_IP = 60;
    public static final Duration DEFAULT_VENTANA_IP = Duration.ofMinutes(1);
    public static final int DEFAULT_MAX_SOLICITUDES_CLAVE = 10;
    public static final Duration DEFAULT_VENTANA_CLAVE = Duration.ofMinutes(1);

    public RateLimitProperties {
        if (maxSolicitudesPorIp <= 0) {
            maxSolicitudesPorIp = DEFAULT_MAX_SOLICITUDES_IP;
        }
        if (ventanaIp == null || ventanaIp.isZero() || ventanaIp.isNegative()) {
            ventanaIp = DEFAULT_VENTANA_IP;
        }
        if (maxSolicitudesPorClave <= 0) {
            maxSolicitudesPorClave = DEFAULT_MAX_SOLICITUDES_CLAVE;
        }
        if (ventanaClave == null || ventanaClave.isZero() || ventanaClave.isNegative()) {
            ventanaClave = DEFAULT_VENTANA_CLAVE;
        }
    }

    public static RateLimitProperties porDefecto() {
        return new RateLimitProperties(
                DEFAULT_MAX_SOLICITUDES_IP,
                DEFAULT_VENTANA_IP,
                DEFAULT_MAX_SOLICITUDES_CLAVE,
                DEFAULT_VENTANA_CLAVE);
    }
}
