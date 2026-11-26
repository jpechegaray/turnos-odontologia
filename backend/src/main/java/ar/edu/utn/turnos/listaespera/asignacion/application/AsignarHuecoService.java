package ar.edu.utn.turnos.listaespera.asignacion.application;

import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.infrastructure.ConsultaOcupacionDisponibilidad;
import ar.edu.utn.turnos.listaespera.domain.CompatibilidadListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.listaespera.domain.Hueco;
import ar.edu.utn.turnos.listaespera.domain.PrestacionParaHueco;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignación manual de un hueco a una entrada de la lista de espera (C-43, design D5). En una única
 * transacción bloquea la entrada, valida compatibilidad (RN-22), resuelve el sillón (design D4), crea el turno
 * con todas las reglas del alta, cierra la entrada y toma las ofertas del hueco. Si el alta falla, la
 * transacción revierte y la entrada y las ofertas quedan como estaban.
 */
@Service
public class AsignarHuecoService {
    private final ListaEsperaPersistencia entradas;
    private final OfertasHuecoPersistencia ofertas;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion profesionalPrestaciones;
    private final DisponibilidadService disponibilidad;
    private final SillonConsulta sillones;
    private final CrearTurnoService crearTurno;
    private final ConsultaOcupacionDisponibilidad ocupacion;

    AsignarHuecoService(ListaEsperaPersistencia entradas, OfertasHuecoPersistencia ofertas,
            ConsultaPrestaciones prestaciones, ProfesionalRealizaPrestacion profesionalPrestaciones,
            DisponibilidadService disponibilidad, SillonConsulta sillones, CrearTurnoService crearTurno,
            ConsultaOcupacionDisponibilidad ocupacion) {
        this.entradas = entradas;
        this.ofertas = ofertas;
        this.prestaciones = prestaciones;
        this.profesionalPrestaciones = profesionalPrestaciones;
        this.disponibilidad = disponibilidad;
        this.sillones = sillones;
        this.crearTurno = crearTurno;
        this.ocupacion = ocupacion;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional
    public Turno asignar(Long entradaId, SolicitudAsignacion solicitud, Long creadoPorUsuarioId) {
        var entrada = entradas.bloquear(entradaId).orElseThrow(() -> new ReglaNegocioException(
                "ENTRADA_NO_ENCONTRADA", EstadoError.NO_ENCONTRADO_404,
                "No se encontró la entrada de la lista de espera"));
        if (entrada.estado() != EstadoListaEspera.ACTIVA) {
            throw new ReglaNegocioException("ENTRADA_NO_ACTIVA", EstadoError.CONFLICTO_409,
                    "La entrada de la lista de espera ya no está activa");
        }
        var prestacion = prestaciones.buscarPorId(entrada.prestacionId()).orElseThrow(() -> new ReglaNegocioException(
                "PRESTACION_INEXISTENTE", EstadoError.NO_PROCESABLE_422, "No se encontró la prestación"));
        int duracion = prestacion.duracionDefaultMin();
        var fin = solicitud.inicio().plus(Duration.ofMinutes(duracion));
        var hueco = new Hueco(solicitud.profesionalId(), solicitud.inicio(), fin);
        boolean realiza = profesionalPrestaciones.realiza(solicitud.profesionalId(), entrada.prestacionId());
        if (!CompatibilidadListaEspera.esCompatible(entrada, hueco, new PrestacionParaHueco(duracion, prestacion.activa()),
                realiza)) {
            throw new ReglaNegocioException("ENTRADA_INCOMPATIBLE", EstadoError.NO_PROCESABLE_422,
                    "La entrada no es compatible con el hueco indicado");
        }
        Long sillonId = resolverSillon(entrada, solicitud, fin);
        var turno = crearTurno.crear(new SolicitudTurno(entrada.pacienteId(), solicitud.profesionalId(), sillonId,
                entrada.prestacionId(), solicitud.inicio(), null), solicitud.notas(), creadoPorUsuarioId);
        entradas.guardar(entrada.asignar(turno.id()));
        ofertas.tomarDelHueco(solicitud.profesionalId(), solicitud.inicio());
        return turno;
    }

    /** Sillón indicado, si no el sugerido por la oferta que siga libre, si no el primero activo libre (design D4). */
    private Long resolverSillon(EntradaListaEspera entrada, SolicitudAsignacion solicitud, Instant fin) {
        if (solicitud.sillonId() != null) {
            return solicitud.sillonId();
        }
        List<Long> libres = disponibilidad.consultar(solicitud.profesionalId(), entrada.prestacionId(),
                solicitud.inicio(), fin, null).stream()
                .filter(horario -> horario.inicio().equals(solicitud.inicio()))
                .map(HorarioDisponible::sillonId)
                .toList();
        var sugerido = ofertas.sugeridaPara(entrada.id(), solicitud.profesionalId(), solicitud.inicio())
                .map(OfertaHueco::sillonSugeridoId).orElse(null);
        if (sugerido != null && libres.contains(sugerido)) {
            return sugerido;
        }
        if (!libres.isEmpty()) {
            // La disponibilidad informa un solo sillón por horario (el de menor id): el sugerido se revisa aparte.
            if (sugerido != null && sillonActivoYLibre(sugerido, solicitud.profesionalId(), solicitud.inicio(), fin)) {
                return sugerido;
            }
            return libres.stream().min(Long::compareTo).orElseThrow();
        }
        if (sugerido != null && sillones.buscarPorId(sugerido).map(SillonConsulta.Datos::activo).orElse(false)) {
            return sugerido;
        }
        return sillones.listarActivos().stream()
                .map(SillonConsulta.Datos::id).min(Long::compareTo)
                .orElseThrow(() -> new ReglaNegocioException("SIN_SILLON_DISPONIBLE", EstadoError.NO_PROCESABLE_422,
                        "No hay sillón disponible"));
    }

    private boolean sillonActivoYLibre(Long sillonId, Long profesionalId, Instant inicio, Instant fin) {
        if (!sillones.buscarPorId(sillonId).map(SillonConsulta.Datos::activo).orElse(false)) {
            return false;
        }
        var intervalo = new Intervalo(inicio, fin);
        return ocupacion.buscar(profesionalId, List.of(sillonId), intervalo).stream()
                .noneMatch(t -> sillonId.equals(t.sillonId()) && t.estado().ocupaLugar()
                        && t.intervalo().solapaCon(intervalo));
    }
}
