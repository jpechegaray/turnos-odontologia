package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;

/** Errores de la consulta y la toma de una oferta de hueco (C-42; RN-15, RN-24, RN-38). Nunca llevan el token. */
public final class ErroresOferta {
    public static final String NO_ENCONTRADA = "OFERTA_NO_ENCONTRADA";
    public static final String HUECO_YA_TOMADO = "HUECO_YA_TOMADO";
    public static final String VENCIDA = "OFERTA_VENCIDA";
    public static final String NO_DISPONIBLE = "OFERTA_NO_DISPONIBLE";

    private ErroresOferta() {
    }

    /** 404 uniforme: token inexistente, mal formado o de otro recurso (RN-38). */
    public static ReglaNegocioException noEncontrada() {
        return new ReglaNegocioException(NO_ENCONTRADA, EstadoError.NO_ENCONTRADO_404, "Oferta no encontrada");
    }

    public static ReglaNegocioException huecoYaTomado() {
        return new ReglaNegocioException(HUECO_YA_TOMADO, EstadoError.CONFLICTO_409,
                "El hueco ya fue tomado.");
    }

    public static ReglaNegocioException vencida() {
        return new ReglaNegocioException(VENCIDA, EstadoError.CONFLICTO_409,
                "La oferta venció: el horario ya empezó.");
    }

    public static ReglaNegocioException noDisponible() {
        return new ReglaNegocioException(NO_DISPONIBLE, EstadoError.CONFLICTO_409,
                "La oferta ya no está disponible.");
    }

    static boolean esPropio(ReglaNegocioException error) {
        return switch (error.codigo()) {
            case NO_ENCONTRADA, HUECO_YA_TOMADO, VENCIDA, NO_DISPONIBLE -> true;
            default -> false;
        };
    }
}
