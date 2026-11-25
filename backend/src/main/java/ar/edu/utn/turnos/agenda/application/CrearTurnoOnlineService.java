package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.ConsultasAgenda;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.agenda.domain.ValidadorTurno;
import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta interna de un turno online (C-42; la reutilizará la reserva online de C-41): canal ONLINE, duración por
 * defecto de la prestación y sillón elegido por el sistema (SU-14). No exige rol: solo la invocan flujos públicos
 * que ya identificaron al paciente por su token. Las reglas y la garantía ante concurrencia son las del alta del
 * personal ({@link AltaTurno}).
 *
 * <p>La elección del sillón se hace leyendo la ocupación sin bloquear; después {@link AltaTurno} bloquea profesional
 * y sillón elegido y revalida, así que una carrera termina en el mismo 409 RN-01 y nunca en un solape.
 */
@Service
public class CrearTurnoOnlineService {
    private final AltaTurno alta;
    private final SillonConsulta sillones;
    private final ConsultaPrestaciones prestaciones;
    private final ConsultasAgenda consultas;
    private final TurnosPersistencia turnos;

    CrearTurnoOnlineService(AltaTurno alta, SillonConsulta sillones, ConsultaPrestaciones prestaciones,
            ConsultasAgenda consultas, TurnosPersistencia turnos) {
        this.alta = alta;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
        this.consultas = consultas;
        this.turnos = turnos;
    }

    /**
     * Ordena por profesional, hasta el fin de la transacción en curso, a quien va a dar un turno online y antes
     * necesita bloquear filas propias (la toma de una oferta bloquea la oferta y la entrada). Tomar este lock
     * primero evita el ciclo "uno espera el profesional, el otro una fila que el primero tiene" (C-42).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void ordenarPorProfesional(Long profesionalId) {
        turnos.bloquearProfesional(profesionalId);
    }

    /**
     * @param sillonPreferidoId sillón a usar si está activo y libre (el del turno liberado); puede ser null
     * @throws ar.edu.utn.turnos.shared.error.ReglaNegocioException 409 RN-01 si el profesional ya tiene un turno o
     *         si ningún sillón activo queda libre; 422 si falla otra regla del alta
     */
    @Transactional
    public Turno crear(Long pacienteId, Long profesionalId, Long prestacionId, Instant inicio, Long sillonPreferidoId) {
        var candidatos = candidatos(sillonPreferidoId);
        if (candidatos.isEmpty()) {
            throw ValidadorTurno.solapado("SILLON");
        }
        // Sin un sillón libre se valida con el primer candidato: el validador informa PROFESIONAL si el profesional
        // está ocupado y, si no, SILLON (todos ocupados).
        Long sillon = intervalo(prestacionId, inicio)
                .flatMap(intervalo -> primeroLibre(candidatos, profesionalId, intervalo))
                .orElse(candidatos.get(0));
        var solicitud = new SolicitudTurno(pacienteId, profesionalId, sillon, prestacionId, inicio, null);
        return alta.registrar(solicitud, (validado, ahora) -> Turno.nuevoOnline(pacienteId, profesionalId, sillon,
                prestacionId, validado, ahora));
    }

    /** El preferido primero (si está activo) y después los activos por id ascendente. */
    private List<Long> candidatos(Long sillonPreferidoId) {
        var activos = sillones.listarActivos().stream().map(SillonConsulta.Datos::id).sorted().toList();
        var orden = new ArrayList<Long>();
        if (sillonPreferidoId != null && activos.contains(sillonPreferidoId)) {
            orden.add(sillonPreferidoId);
        }
        activos.stream().filter(id -> !id.equals(sillonPreferidoId)).forEach(orden::add);
        return orden;
    }

    /** Intervalo con la duración por defecto; vacío si la prestación no existe (lo rechaza el validador). */
    private Optional<Intervalo> intervalo(Long prestacionId, Instant inicio) {
        return prestaciones.buscarPorId(prestacionId)
                .map(PrestacionResumen::duracionDefaultMin)
                .filter(minutos -> minutos > 0)
                .map(minutos -> Intervalo.desde(inicio, minutos));
    }

    private Optional<Long> primeroLibre(List<Long> candidatos, Long profesionalId, Intervalo intervalo) {
        return candidatos.stream().filter(sillon -> libre(sillon, profesionalId, intervalo)).findFirst();
    }

    private boolean libre(Long sillon, Long profesionalId, Intervalo intervalo) {
        return consultas.turnosQueSolapan(profesionalId, sillon, intervalo).stream()
                .filter(t -> t.estado().ocupaLugar())
                .filter(t -> t.intervalo().solapaCon(intervalo))
                .map(TurnoOcupado::sillonId)
                .noneMatch(sillon::equals);
    }
}
