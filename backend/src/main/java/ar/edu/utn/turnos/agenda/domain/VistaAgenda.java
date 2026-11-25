package ar.edu.utn.turnos.agenda.domain;

import java.util.Locale;

/** Vista de la agenda (US-013): un día o la semana de lunes a domingo. */
public enum VistaAgenda {
    DIA,
    SEMANA;

    /** Lee {@code dia} o {@code semana} sin distinguir mayúsculas; cualquier otro valor se rechaza. */
    public static VistaAgenda desdeTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("La vista es obligatoria");
        }
        return switch (texto.strip().toLowerCase(Locale.ROOT)) {
            case "dia" -> DIA;
            case "semana" -> SEMANA;
            default -> throw new IllegalArgumentException("La vista debe ser dia o semana");
        };
    }
}
