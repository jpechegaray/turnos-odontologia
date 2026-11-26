package ar.edu.utn.turnos.reservapublica.reserva.application;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;

/**
 * Consentimiento del tratamiento de datos de la reserva online (RN-26, PA-08). La versión vigente identifica
 * el texto que muestra el formulario (C-48): cambiar el texto obliga a cambiar esta constante en el mismo PR.
 */
public final class ConsentimientoDatos {

    public static final String VERSION_VIGENTE = "2026-11-v1";
    static final String VERSION_NO_VIGENTE = "CONSENTIMIENTO_VERSION_NO_VIGENTE";

    private ConsentimientoDatos() {
    }

    /** 422 {@code RN-26} si no acepta y 422 {@code CONSENTIMIENTO_VERSION_NO_VIGENTE} si aceptó otro texto. */
    public static void exigir(Boolean acepta, String version) {
        if (!Boolean.TRUE.equals(acepta)) {
            throw new ReglaNegocioException("RN-26", EstadoError.NO_PROCESABLE_422,
                    "Para reservar hay que aceptar el tratamiento de los datos personales");
        }
        if (!VERSION_VIGENTE.equals(version)) {
            throw new ReglaNegocioException(VERSION_NO_VIGENTE, EstadoError.NO_PROCESABLE_422,
                    "El texto de consentimiento aceptado no es el vigente. Recargá la página y volvé a aceptarlo.");
        }
    }
}
