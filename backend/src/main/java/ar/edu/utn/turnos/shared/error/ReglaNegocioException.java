package ar.edu.utn.turnos.shared.error;

import java.util.Map;
import java.util.Objects;

/**
 * Incumplimiento de una regla de negocio. Los módulos la lanzan (o la extienden) y el
 * {@code ManejadorErrores} la traduce a Problem Details sin tocar el manejador.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;
    private final EstadoError estado;
    private final Map<String, Object> propiedades;

    public ReglaNegocioException(String codigo, EstadoError estado, String mensaje) {
        this(codigo, estado, mensaje, Map.of());
    }

    /**
     * @param codigo      código de la regla, por ejemplo {@code RN-04}
     * @param estado      estado de error asociado a la regla
     * @param mensaje     texto del {@code detail}; lo escribe quien programa y no puede llevar datos personales
     * @param propiedades datos de contexto que se agregan al cuerpo del error (por ejemplo {@code recurso})
     */
    public ReglaNegocioException(String codigo, EstadoError estado, String mensaje, Map<String, Object> propiedades) {
        super(mensaje);
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.propiedades = Map.copyOf(propiedades);
    }

    public String codigo() {
        return codigo;
    }

    public EstadoError estado() {
        return estado;
    }

    public Map<String, Object> propiedades() {
        return propiedades;
    }
}
