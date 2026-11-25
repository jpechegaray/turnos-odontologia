package ar.edu.utn.turnos.agenda.domain;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Turno de la agenda (KB 04 §Turno). Inmutable: cada transición devuelve un turno nuevo. Referencia a
 * paciente, profesional, sillón, prestación y usuario solo por id (KB 08). Incluye desde ya las
 * columnas de cancelación y reprogramación para que C-27 y C-35 no reabran el modelo.
 */
public record Turno(
        Long id,
        Long pacienteId,
        Long profesionalId,
        Long sillonId,
        Long prestacionId,
        Intervalo intervalo,
        int duracionMin,
        EstadoTurno estado,
        CanalTurno canal,
        String notas,
        boolean cancelacionTardia,
        boolean reprogramadoTarde,
        Instant canceladoEn,
        ActorCancelacion canceladoPor,
        Long canceladoPorUsuarioId,
        String motivoCancelacion,
        Long creadoPorUsuarioId,
        Instant creadoEn,
        Long version) {

    public Turno {
        Objects.requireNonNull(pacienteId, "pacienteId");
        Objects.requireNonNull(profesionalId, "profesionalId");
        Objects.requireNonNull(sillonId, "sillonId");
        Objects.requireNonNull(prestacionId, "prestacionId");
        Objects.requireNonNull(intervalo, "intervalo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(creadoEn, "creadoEn");
        if (Duration.between(intervalo.inicio(), intervalo.fin()).toMinutes() != duracionMin) {
            throw new IllegalArgumentException("La duración debe coincidir con el intervalo (fin = inicio + duración)");
        }
    }

    /** Turno dado por el personal: nace PROGRAMADO y con canal INTERNO (Flujo 1). */
    public static Turno nuevo(Long pacienteId, Long profesionalId, Long sillonId, Long prestacionId,
            Intervalo intervalo, String notas, Long creadoPorUsuarioId, Instant creadoEn) {
        int duracion = Math.toIntExact(Duration.between(intervalo.inicio(), intervalo.fin()).toMinutes());
        return new Turno(null, pacienteId, profesionalId, sillonId, prestacionId, intervalo, duracion,
                EstadoTurno.PROGRAMADO, CanalTurno.INTERNO, notas, false, false, null, null, null, null,
                creadoPorUsuarioId, creadoEn, null);
    }

    /** Aplica la tabla de transiciones de RN-12; una transición no permitida es un conflicto (409). */
    public Turno pasarA(EstadoTurno destino) {
        if (!estado.puedePasarA(destino)) {
            throw new ReglaNegocioException("TURNO_TRANSICION_INVALIDA", EstadoError.CONFLICTO_409,
                    "El turno no puede pasar de " + estado + " a " + destino);
        }
        return new Turno(id, pacienteId, profesionalId, sillonId, prestacionId, intervalo, duracionMin, destino,
                canal, notas, cancelacionTardia, reprogramadoTarde, canceladoEn, canceladoPor, canceladoPorUsuarioId,
                motivoCancelacion, creadoPorUsuarioId, creadoEn, version);
    }

    /**
     * Cancela el turno (RN-05, RN-12, RN-19). Primero aplica la transición, así un turno en estado final
     * responde 409 aunque ya haya pasado; después rechaza con 422 un turno que ya empezó (a la hora exacta
     * de inicio ya empezó). Registra quién, cuándo, el motivo y si la cancelación es tardía.
     *
     * @param usuarioId usuario del personal que cancela; null cuando el actor es el paciente por token
     */
    public Turno cancelar(ActorCancelacion actor, Long usuarioId, String motivo, Instant ahora, Duration plazo) {
        Objects.requireNonNull(actor, "actor");
        var cancelado = pasarA(EstadoTurno.CANCELADO);
        if (!ahora.isBefore(intervalo.inicio())) {
            throw new ReglaNegocioException("RN-05", EstadoError.NO_PROCESABLE_422,
                    "No se puede cancelar un turno que ya empezó; se registra la asistencia (RN-20)");
        }
        boolean tardia = PoliticaCancelacion.esTardia(intervalo.inicio(), ahora, plazo);
        return new Turno(id, pacienteId, profesionalId, sillonId, prestacionId, intervalo, duracionMin,
                cancelado.estado(), canal, notas, tardia, reprogramadoTarde, ahora, actor,
                actor == ActorCancelacion.USUARIO ? usuarioId : null, motivo, creadoPorUsuarioId, creadoEn, version);
    }
}
