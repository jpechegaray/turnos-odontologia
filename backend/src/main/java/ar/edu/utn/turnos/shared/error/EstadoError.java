package ar.edu.utn.turnos.shared.error;

/**
 * Estados HTTP admitidos para errores de regla de negocio. Es un enum propio (y no {@code HttpStatus})
 * para que las capas {@code domain} puedan lanzar {@link ReglaNegocioException} sin depender de Spring.
 * El manejador de errores lo traduce al estado HTTP real.
 */
public enum EstadoError {

    PROHIBIDO_403(403),
    NO_ENCONTRADO_404(404),
    CONFLICTO_409(409),
    NO_PROCESABLE_422(422);

    private final int codigoHttp;

    EstadoError(int codigoHttp) {
        this.codigoHttp = codigoHttp;
    }

    public int codigoHttp() {
        return codigoHttp;
    }
}
