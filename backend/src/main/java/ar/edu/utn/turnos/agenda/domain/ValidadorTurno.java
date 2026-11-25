package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas de creación de un turno (Flujo 1, paso 6), en orden: RN-11, RN-13, RN-09, RN-16/RN-04, RN-04 y
 * RN-01. Corta en la primera que falla. Es puro: no conoce Spring ni la base; lo que necesita de la agenda
 * lo pregunta a {@link ConsultasAgenda}, y solo después de que pasaron las reglas que no la necesitan.
 */
public final class ValidadorTurno {
    private final Clock reloj;

    public ValidadorTurno(Clock reloj) {
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * @param turnoExcluidoId turno a ignorar al buscar solapamientos (lo usa la reprogramación, RN-18); null al crear
     * @return el intervalo efectivo del turno, con la duración de la solicitud o la de la prestación
     */
    public Intervalo validar(SolicitudTurno solicitud, RecursosTurno recursos, ConsultasAgenda consultas,
            Long turnoExcluidoId) {
        validarRecursos(recursos);
        validarNoPasado(solicitud.inicio());
        int duracion = duracionEfectiva(solicitud, recursos);
        validarGrilla(solicitud.inicio());
        var intervalo = Intervalo.desde(solicitud.inicio(), duracion);
        validarHorarioYBloqueos(solicitud.profesionalId(), intervalo, consultas);
        validarSinSolapamiento(solicitud, intervalo, consultas, turnoExcluidoId);
        return intervalo;
    }

    /**
     * RN-01, RN-02 y RN-10: ningún turno que ocupe lugar del mismo profesional o del mismo sillón comparte
     * un instante con el nuevo. Se revisa primero el profesional, así que si chocan los dos ejes se informa
     * PROFESIONAL. No se confía en que la consulta haya filtrado: se vuelve a comparar acá.
     */
    private static void validarSinSolapamiento(SolicitudTurno solicitud, Intervalo intervalo,
            ConsultasAgenda consultas, Long turnoExcluidoId) {
        var candidatos = consultas.turnosQueSolapan(solicitud.profesionalId(), solicitud.sillonId(), intervalo).stream()
                .filter(t -> t.estado().ocupaLugar())
                .filter(t -> !t.turnoId().equals(turnoExcluidoId))
                .filter(t -> t.intervalo().solapaCon(intervalo))
                .toList();
        if (candidatos.stream().anyMatch(t -> t.profesionalId().equals(solicitud.profesionalId()))) {
            throw solapado("PROFESIONAL");
        }
        if (candidatos.stream().anyMatch(t -> t.sillonId().equals(solicitud.sillonId()))) {
            throw solapado("SILLON");
        }
    }

    /** El mismo error que da la persistencia cuando la base rechaza el solapamiento (RN-15). */
    public static ReglaNegocioException solapado(String recurso) {
        return new ReglaNegocioException("RN-01", EstadoError.CONFLICTO_409,
                "El horario se superpone con otro turno. Elegí otro horario.", Map.of("recurso", recurso));
    }

    /** RN-16 y RN-04: completo en un tramo del profesional y sin tocar sus bloqueos. */
    private static void validarHorarioYBloqueos(Long profesionalId, Intervalo intervalo, ConsultasAgenda consultas) {
        if (!consultas.dentroDeHorario(profesionalId, intervalo)) {
            throw noProcesable("RN-04", "El turno queda fuera del horario de atención del profesional",
                    "FUERA_DE_HORARIO");
        }
        if (consultas.enBloqueo(profesionalId, intervalo)) {
            throw noProcesable("RN-04", "El profesional tiene la agenda bloqueada en ese horario", "BLOQUEO");
        }
    }

    /** RN-13: el turno empieza después del momento actual; empezar justo ahora tampoco vale. */
    private void validarNoPasado(Instant inicio) {
        if (!inicio.isAfter(reloj.instant())) {
            throw noProcesable("RN-13", "No se pueden dar turnos en el pasado");
        }
    }

    /**
     * RN-03 y RN-09: la de la solicitud o, si falta, la de la prestación. {@link DuracionPrestacion} ya
     * rechaza con RN-09 lo que no sea múltiplo de 5 entre 5 y 480.
     */
    private static int duracionEfectiva(SolicitudTurno solicitud, RecursosTurno recursos) {
        int minutos = solicitud.duracionMin() != null ? solicitud.duracionMin() : recursos.duracionDefaultMin();
        return new DuracionPrestacion(minutos).minutos();
    }

    /** RN-09: el inicio cae en un minuto múltiplo de 5, en punto (sin segundos), en hora de Buenos Aires. */
    private static void validarGrilla(Instant inicio) {
        var local = inicio.atZone(ZonaHoraria.BUENOS_AIRES);
        if (local.getMinute() % DuracionPrestacion.GRANULARIDAD != 0 || local.getSecond() != 0 || local.getNano() != 0) {
            throw noProcesable("RN-09", "El turno tiene que empezar en un minuto múltiplo de 5");
        }
    }

    /** RN-11: los cuatro recursos existen y están activos, y el profesional realiza la prestación. */
    private static void validarRecursos(RecursosTurno recursos) {
        validarRecurso(recursos.paciente(), "PACIENTE", "el paciente", "INACTIVO");
        validarRecurso(recursos.profesional(), "PROFESIONAL", "el profesional", "INACTIVO");
        validarRecurso(recursos.sillon(), "SILLON", "el sillón", "INACTIVO");
        validarRecurso(recursos.prestacion(), "PRESTACION", "la prestación", "INACTIVA");
        if (!recursos.profesionalRealizaPrestacion()) {
            throw noProcesable("RN-11", "El profesional no realiza esa prestación");
        }
    }

    private static void validarRecurso(EstadoRecurso estado, String recurso, String nombre, String inactivo) {
        switch (estado) {
            case INEXISTENTE -> throw noProcesable(recurso + "_INEXISTENTE", "No se encontró " + nombre);
            case INACTIVO -> throw noProcesable(recurso + "_" + inactivo, "Está inactivo o dado de baja: " + nombre);
            case ACTIVO -> {
            }
        }
    }

    private static ReglaNegocioException noProcesable(String codigo, String mensaje) {
        return new ReglaNegocioException(codigo, EstadoError.NO_PROCESABLE_422, mensaje);
    }

    private static ReglaNegocioException noProcesable(String codigo, String mensaje, String motivo) {
        return new ReglaNegocioException(codigo, EstadoError.NO_PROCESABLE_422, mensaje, Map.of("motivo", motivo));
    }
}
