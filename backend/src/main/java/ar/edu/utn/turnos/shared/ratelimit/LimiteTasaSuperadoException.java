package ar.edu.utn.turnos.shared.ratelimit;

/**
 * Excepción lanzada cuando una clave pública supera el límite de tasa configurado (RN-27).
 * Nunca incluye la clave ni datos personales en el mensaje ni en {@link #toString()} (RN-35).
 */
public final class LimiteTasaSuperadoException extends RuntimeException {

    public static final String CODIGO_REGLA = "RN-27";
    public static final String DETALLE_LIMITE_SUPERADO =
            "Se superó el límite de solicitudes permitidas. Intente nuevamente más tarde.";

    private final long segundosReintento;

    public LimiteTasaSuperadoException(long segundosReintento) {
        super(DETALLE_LIMITE_SUPERADO);
        this.segundosReintento = Math.max(1L, segundosReintento);
    }

    public String codigo() {
        return CODIGO_REGLA;
    }

    public long segundosReintento() {
        return segundosReintento;
    }
}
