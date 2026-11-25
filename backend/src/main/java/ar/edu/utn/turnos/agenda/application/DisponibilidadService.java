package ar.edu.utn.turnos.agenda.application;

import ar.edu.utn.turnos.agenda.domain.CalculadorDisponibilidad;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import ar.edu.utn.turnos.agenda.infrastructure.ConsultaOcupacionDisponibilidad;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.profesionales.application.AgendaProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Núcleo interno reutilizable por recepción y por los futuros flujos de reserva pública. */
@Service
public class DisponibilidadService {
    private final ProfesionalConsulta profesionales;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion asignaciones;
    private final SillonConsulta sillones;
    private final AgendaProfesionalConsulta agenda;
    private final ConsultaOcupacionDisponibilidad ocupacion;
    private final Clock reloj;
    private final CalculadorDisponibilidad calculador = new CalculadorDisponibilidad();

    public DisponibilidadService(ProfesionalConsulta profesionales, ConsultaPrestaciones prestaciones,
            ProfesionalRealizaPrestacion asignaciones, SillonConsulta sillones, AgendaProfesionalConsulta agenda,
            ConsultaOcupacionDisponibilidad ocupacion, Clock reloj) {
        this.profesionales = profesionales;
        this.prestaciones = prestaciones;
        this.asignaciones = asignaciones;
        this.sillones = sillones;
        this.agenda = agenda;
        this.ocupacion = ocupacion;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<HorarioDisponible> consultar(Long profesionalId, Long prestacionId, Instant desde, Instant hasta,
            Integer duracionMin) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)
                || Duration.between(desde, hasta).compareTo(Duration.ofDays(31)) > 0) {
            throw rangoInvalido();
        }
        validarCalendario(desde, hasta);
        var profesional = profesionales.buscarPorId(profesionalId)
                .orElseThrow(() -> error("PROFESIONAL_INEXISTENTE", "No se encontró el profesional"));
        if (!profesional.activo()) {
            throw error("PROFESIONAL_INACTIVO", "El profesional está inactivo");
        }
        var prestacion = prestaciones.buscarPorId(prestacionId)
                .orElseThrow(() -> error("PRESTACION_INEXISTENTE", "No se encontró la prestación"));
        if (!prestacion.activa()) {
            throw error("PRESTACION_INACTIVA", "La prestación está inactiva");
        }
        if (!asignaciones.realiza(profesionalId, prestacionId)) {
            throw error("RN-11", "El profesional no realiza esa prestación");
        }
        int duracion = new DuracionPrestacion(duracionMin == null ? prestacion.duracionDefaultMin() : duracionMin).minutos();
        var ahora = reloj.instant();
        if (!hasta.isAfter(ahora)) {
            return List.of();
        }
        var rango = new Intervalo(desde, hasta);
        var activos = sillones.listarActivos().stream().map(SillonConsulta.Datos::id).sorted().toList();
        if (activos.isEmpty()) {
            return List.of();
        }
        try {
            var tramos = materializar(profesionalId, rango);
            return calculador.calcular(profesionalId, rango, duracion, ahora, tramos, agenda.bloqueos(profesionalId, rango),
                    ocupacion.buscar(profesionalId, activos, rango), activos);
        } catch (DateTimeException ex) {
            throw rangoInvalido();
        }
    }

    private List<Intervalo> materializar(Long profesionalId, Intervalo rango) {
        var horarios = agenda.horarios(profesionalId);
        var tramos = new ArrayList<Intervalo>();
        var fecha = rango.inicio().atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate();
        var ultima = rango.fin().atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate();
        while (true) {
            for (var horario : horarios) {
                if (horario.diaSemana() == fecha.getDayOfWeek().getValue()) {
                    tramos.add(new Intervalo(fecha.atTime(horario.horaInicio()).atZone(ZonaHoraria.BUENOS_AIRES).toInstant(),
                            fecha.atTime(horario.horaFin()).atZone(ZonaHoraria.BUENOS_AIRES).toInstant()));
                }
            }
            if (fecha.equals(ultima)) {
                return tramos;
            }
            fecha = fecha.plusDays(1);
        }
    }

    private static ReglaNegocioException rangoInvalido() {
        return error("RANGO_DISPONIBILIDAD_INVALIDO",
                "El rango debe tener fin posterior al inicio, abarcar como máximo 31 días y usar años entre 1 y 9999");
    }

    /** Acota el calendario de entrada antes de enviar instantes no representables a PostgreSQL. */
    private static void validarCalendario(Instant desde, Instant hasta) {
        try {
            int primerAnio = desde.atZone(ZonaHoraria.BUENOS_AIRES).getYear();
            int ultimoAnio = hasta.atZone(ZonaHoraria.BUENOS_AIRES).getYear();
            if (primerAnio < 1 || ultimoAnio > 9999) {
                throw rangoInvalido();
            }
        } catch (DateTimeException ex) {
            throw rangoInvalido();
        }
    }

    private static ReglaNegocioException error(String codigo, String detalle) {
        return new ReglaNegocioException(codigo, EstadoError.NO_PROCESABLE_422, detalle);
    }
}
