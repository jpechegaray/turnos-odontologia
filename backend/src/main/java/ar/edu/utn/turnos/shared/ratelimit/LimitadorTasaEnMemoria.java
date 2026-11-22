package ar.edu.utn.turnos.shared.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Limitador de tasa en memoria por ventana fija controlado por {@link Clock} y sin dependencias de
 * frameworks. Purga automáticamente las ventanas vencidas para evitar acumulación en memoria.
 */
public final class LimitadorTasaEnMemoria {

    private final int maximoSolicitudes;
    private final Duration duracionVentana;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Ventana> contadores = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> proximaLimpieza;

    public LimitadorTasaEnMemoria(int maximoSolicitudes, Duration duracionVentana, Clock reloj) {
        if (maximoSolicitudes <= 0) {
            throw new IllegalArgumentException("maximoSolicitudes debe ser positivo");
        }
        Objects.requireNonNull(duracionVentana, "duracionVentana");
        if (duracionVentana.isZero() || duracionVentana.isNegative()) {
            throw new IllegalArgumentException("duracionVentana debe ser positiva");
        }
        this.maximoSolicitudes = maximoSolicitudes;
        this.duracionVentana = duracionVentana;
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.proximaLimpieza = new AtomicReference<>();
    }

    public Resultado evaluar(String clave) {
        Objects.requireNonNull(clave, "clave");
        Instant ahora = reloj.instant();
        limpiarVencidosSiCorresponde(ahora);

        var resultado = new Resultado[1];
        contadores.compute(clave, (k, actual) -> {
            if (actual == null || !ahora.isBefore(actual.venceEn())) {
                Instant venceEn = ahora.plus(duracionVentana);
                resultado[0] = new Resultado(true, 0L);
                return new Ventana(venceEn, 1);
            }
            if (actual.consumidos() < maximoSolicitudes) {
                resultado[0] = new Resultado(true, 0L);
                return new Ventana(actual.venceEn(), actual.consumidos() + 1);
            }
            long restantesMs = Math.max(1L, Duration.between(ahora, actual.venceEn()).toMillis());
            long segundosReintento = Math.max(1L, (restantesMs + 999L) / 1000L);
            resultado[0] = new Resultado(false, segundosReintento);
            return actual;
        });
        return resultado[0];
    }

    public int cantidadContadoresActivos() {
        return contadores.size();
    }

    private void limpiarVencidosSiCorresponde(Instant ahora) {
        Instant limite = proximaLimpieza.get();
        if (limite == null) {
            proximaLimpieza.compareAndSet(null, ahora.plus(duracionVentana));
            return;
        }
        if (!ahora.isBefore(limite)
                && proximaLimpieza.compareAndSet(limite, ahora.plus(duracionVentana))) {
            contadores.entrySet().removeIf(entrada -> !ahora.isBefore(entrada.getValue().venceEn()));
        }
    }

    private record Ventana(Instant venceEn, int consumidos) {
    }

    public record Resultado(boolean permitido, long segundosReintento) {
    }
}
