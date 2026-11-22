package ar.edu.utn.turnos.profesionales.bloqueo.application;

import java.time.Instant;

public record BloqueoDatos(Long id, Long profesionalId, Instant inicio, Instant fin, String motivo) {
}
