package ar.edu.utn.turnos.listaespera.web;

import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Representación de una entrada: solo IDs, sin datos personales del paciente (RN-35). */
record RespuestaEntrada(Long id, Long pacienteId, Long prestacionId, Long profesionalId, String fechaDesde,
        String fechaHasta, String franjaDesde, String franjaHasta, EstadoListaEspera estado, Long turnoAsignadoId,
        Instant creadoEn) {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    static RespuestaEntrada de(EntradaListaEspera e) {
        return new RespuestaEntrada(e.id(), e.pacienteId(), e.prestacionId(), e.profesionalId(),
                e.fechaDesde().toString(), e.fechaHasta().toString(), hora(e.franjaDesde()), hora(e.franjaHasta()),
                e.estado(), e.turnoAsignadoId(), e.creadoEn());
    }

    private static String hora(LocalTime hora) {
        return hora == null ? null : hora.format(HH_MM);
    }
}
