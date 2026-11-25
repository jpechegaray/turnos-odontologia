package ar.edu.utn.turnos.listaespera.domain;

import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Compatibilidad entre un hueco y una entrada de la lista de espera (RN-22). Función pura: los datos de la
 * prestación y si el profesional la realiza llegan como parámetros. No verifica que el hueco esté libre ni
 * dentro del horario: eso lo valida la creación del turno (RN-01, RN-02, RN-04).
 */
public final class CompatibilidadListaEspera {

    private CompatibilidadListaEspera() {
    }

    public static boolean esCompatible(EntradaListaEspera entrada, Hueco hueco, PrestacionParaHueco prestacion,
            boolean profesionalRealiza) {
        if (entrada.estado() != EstadoListaEspera.ACTIVA || !prestacion.activa() || !profesionalRealiza) {
            return false;
        }
        if (entrada.profesionalId() != null && !entrada.profesionalId().equals(hueco.profesionalId())) {
            return false;
        }
        var duracion = Duration.ofMinutes(prestacion.duracionMin());
        if (duracion.compareTo(hueco.duracion()) > 0) {
            return false;
        }
        // El turno resultante empieza al inicio del hueco (SU-14 d); fecha y franja son de pared en Buenos Aires.
        ZonedDateTime inicio = hueco.inicio().atZone(ZonaHoraria.BUENOS_AIRES);
        ZonedDateTime fin = inicio.plus(duracion);
        var fecha = inicio.toLocalDate();
        if (fecha.isBefore(entrada.fechaDesde()) || fecha.isAfter(entrada.fechaHasta())) {
            return false;
        }
        return entrada.franjaDesde() == null || dentroDeFranja(entrada, inicio, fin);
    }

    private static boolean dentroDeFranja(EntradaListaEspera entrada, ZonedDateTime inicio, ZonedDateTime fin) {
        // Si el turno cruza la medianoche no cae en ninguna franja del día.
        if (!fin.toLocalDate().equals(inicio.toLocalDate())) {
            return false;
        }
        return !inicio.toLocalTime().isBefore(entrada.franjaDesde())
                && !fin.toLocalTime().isAfter(entrada.franjaHasta());
    }
}
