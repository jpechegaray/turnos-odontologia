package ar.edu.utn.turnos.configuracion.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.Map;

public enum ParametroConfiguracion {
    PLAZO_CANCELACION("cancelacion.plazo_horas", 24, "Horas mínimas para cancelar sin penalidad", new Rango(0, 168)),
    ANTICIPACION_RECORDATORIO("recordatorio.anticipacion_horas", 24, "Horas de anticipación del recordatorio",
            new Rango(1, 168)),
    HORIZONTE_RESERVA("reserva_online.horizonte_dias", 60, "Días a futuro para reservar en línea"),
    ANTICIPACION_RESERVA("reserva_online.anticipacion_minima_horas", 2, "Horas mínimas para reservar en línea"),
    TOPE_TURNOS_EMAIL("reserva_online.max_turnos_activos_por_email", 3, "Tope de turnos activos por email"),
    TOPE_AVISOS("lista_espera.max_avisos_por_hueco", 10, "Tope de avisos por hueco liberado"),
    NOMBRE_CONSULTORIO("consultorio.nombre", "Consultorio Ficticio", "Nombre público del consultorio");

    private final String clave;
    private final String valorInicial;
    private final String descripcion;
    private final Rango rango;

    ParametroConfiguracion(String clave, int valorInicial, String descripcion) {
        this(clave, valorInicial, descripcion, new Rango(1, Integer.MAX_VALUE));
    }

    ParametroConfiguracion(String clave, int valorInicial, String descripcion, Rango rango) {
        this(clave, Integer.toString(valorInicial), descripcion, rango);
    }

    ParametroConfiguracion(String clave, String valorInicial, String descripcion) {
        this(clave, valorInicial, descripcion, null);
    }

    ParametroConfiguracion(String clave, String valorInicial, String descripcion, Rango rango) {
        this.clave = clave;
        this.valorInicial = valorInicial;
        this.descripcion = descripcion;
        this.rango = rango;
    }

    public String clave() {
        return clave;
    }

    public String valorInicial() {
        return valorInicial;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean esNumerico() {
        return rango != null;
    }

    public Object validar(Object valor) {
        if (!esNumerico()) {
            if (valor instanceof String texto && !texto.isBlank()) {
                if (texto.indexOf(0) >= 0) {
                    throw invalido("No puede contener el carácter nulo");
                }
                return texto;
            }
            throw invalido("Debe ser texto no blanco");
        }
        var numero = switch (valor) {
            case Integer entero -> BigDecimal.valueOf(entero);
            case BigDecimal decimal -> decimal;
            case null, default -> throw invalido("Debe ser un número entero");
        };
        if (numero.compareTo(BigDecimal.valueOf(rango.minimo())) < 0
                || numero.compareTo(BigDecimal.valueOf(rango.maximo())) > 0) {
            throw invalido("Debe ser un entero entre %d y %d".formatted(rango.minimo(), rango.maximo()));
        }
        try {
            return numero.intValueExact();
        } catch (ArithmeticException ex) {
            throw invalido("Debe ser un número entero");
        }
    }

    private ReglaNegocioException invalido(String motivo) {
        return new ReglaNegocioException("RN-36", EstadoError.NO_PROCESABLE_422, motivo, Map.of("campo", clave));
    }

    private record Rango(int minimo, int maximo) {
    }
}
