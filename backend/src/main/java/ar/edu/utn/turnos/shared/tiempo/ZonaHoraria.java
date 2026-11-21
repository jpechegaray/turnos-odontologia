package ar.edu.utn.turnos.shared.tiempo;

import java.time.ZoneId;

/** Zona horaria de negocio (DD-05). Se identifica por nombre de región, nunca por desfase fijo. */
public final class ZonaHoraria {

    public static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    private ZonaHoraria() {
    }
}
