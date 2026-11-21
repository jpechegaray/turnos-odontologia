package ar.edu.utn.turnos.prestaciones.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;

/**
 * Duración por defecto de una prestación en minutos (RN-09): múltiplo de 5 entre 5 y 480 inclusive.
 */
public record DuracionPrestacion(int minutos) {

    public static final int MINIMO = 5;
    public static final int MAXIMO = 480;
    public static final int GRANULARIDAD = 5;

    public DuracionPrestacion {
        if (minutos < MINIMO || minutos > MAXIMO || minutos % GRANULARIDAD != 0) {
            throw new ReglaNegocioException("RN-09", EstadoError.NO_PROCESABLE_422,
                    "La duración debe ser un múltiplo de 5 minutos entre 5 y 480.");
        }
    }
}
