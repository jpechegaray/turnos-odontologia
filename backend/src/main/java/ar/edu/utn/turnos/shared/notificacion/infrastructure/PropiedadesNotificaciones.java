package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de {@code app.notificaciones} (RM-02, dueño C-15). El remitente es obligatorio y el
 * máximo de intentos se acota a 1..3. Los bloques anidados toman los defaults del diseño si faltan.
 */
@ConfigurationProperties(prefix = "app.notificaciones")
@Validated
public record PropiedadesNotificaciones(
        String canal,
        @NotBlank String remitente,
        @Valid Planificador planificador,
        @Valid Reintento reintento) {

    public static final String CANAL_SMTP = "smtp";
    public static final String CANAL_MEMORIA = "memoria";

    public PropiedadesNotificaciones {
        canal = (canal == null || canal.isBlank()) ? CANAL_SMTP : canal;
        planificador = planificador == null ? new Planificador(true, Duration.ofMinutes(5)) : planificador;
        reintento = reintento == null ? Reintento.porDefecto() : reintento;
    }

    /** Indica si el canal activo es el adaptador en memoria. */
    public boolean usaCanalMemoria() {
        return CANAL_MEMORIA.equalsIgnoreCase(canal);
    }

    /** Configuración del planificador periódico. */
    public record Planificador(boolean habilitado, Duration intervalo) {

        public Planificador {
            if (intervalo == null || intervalo.isZero() || intervalo.isNegative()) {
                intervalo = Duration.ofMinutes(5);
            }
        }
    }

    /** Política de reintentos: máximo de intentos, espera inicial y multiplicador. */
    public record Reintento(
            @Min(1) @Max(3) int maximoIntentos,
            Duration esperaInicial,
            int multiplicador) {

        public Reintento {
            if (esperaInicial == null || esperaInicial.isZero() || esperaInicial.isNegative()) {
                esperaInicial = Duration.ofMinutes(5);
            }
            if (multiplicador < 1) {
                multiplicador = 2;
            }
        }

        public static Reintento porDefecto() {
            return new Reintento(3, Duration.ofMinutes(5), 2);
        }
    }
}
