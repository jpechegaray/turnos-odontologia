package ar.edu.utn.turnos.reservapublica.application;

import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Disponibilidad para la reserva online (Flujo 4, RN-08, RN-27). Fachada pública, sin autorización,
 * sobre el núcleo {@link DisponibilidadService}: valida el rango pedido, luego los recursos (antes del
 * recorte, para que un recurso no disponible nunca se confunda con un rango vacío), recorta a la
 * anticipación mínima y al horizonte vigentes y quita el sillón de la respuesta.
 *
 * <p>Cualquier recurso no disponible da el mismo 404 para no revelar por qué (ni si existe).
 */
@Service
public class DisponibilidadPublica {

    public static final String RECURSO_NO_DISPONIBLE = "RECURSO_NO_DISPONIBLE";
    public static final String MENSAJE_NO_DISPONIBLE =
            "El profesional o la prestación no están disponibles para reservar online";
    static final String RANGO_INVALIDO = "RANGO_DISPONIBILIDAD_INVALIDO";
    static final Duration RANGO_MAXIMO = Duration.ofDays(31);

    /** Códigos de recurso del núcleo que se traducen por si cambia su orden de validación o hay una carrera. */
    private static final Set<String> CODIGOS_RECURSO = Set.of("PROFESIONAL_INEXISTENTE", "PROFESIONAL_INACTIVO",
            "PRESTACION_INEXISTENTE", "PRESTACION_INACTIVA", "RN-11");

    private final DisponibilidadService nucleo;
    private final ProfesionalConsulta profesionales;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion asignaciones;
    private final ConfiguracionServicio configuracion;
    private final Clock reloj;

    public DisponibilidadPublica(DisponibilidadService nucleo, ProfesionalConsulta profesionales,
            ConsultaPrestaciones prestaciones, ProfesionalRealizaPrestacion asignaciones,
            ConfiguracionServicio configuracion, Clock reloj) {
        this.nucleo = nucleo;
        this.profesionales = profesionales;
        this.prestaciones = prestaciones;
        this.asignaciones = asignaciones;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    /** @return los horarios libres {@code [inicio, fin)} dentro de los límites de la reserva online */
    public List<Intervalo> consultar(Long profesionalId, Long prestacionId, Instant desde, Instant hasta) {
        validarRango(desde, hasta);
        validarRecursos(profesionalId, prestacionId);
        var ahora = reloj.instant();
        var minimo = ahora.plus(Duration.ofHours(configuracion.anticipacionMinimaReservaOnlineHoras()));
        var maximo = ahora.plus(Duration.ofDays(configuracion.horizonteReservaOnlineDias()));
        var inicio = desde.isAfter(minimo) ? desde : minimo;
        var fin = hasta.isBefore(maximo) ? hasta : maximo;
        if (!fin.isAfter(inicio)) {
            return List.of();
        }
        try {
            return nucleo.consultar(profesionalId, prestacionId, inicio, fin, null).stream()
                    .map(horario -> new Intervalo(horario.inicio(), horario.fin()))
                    .toList();
        } catch (ReglaNegocioException ex) {
            if (CODIGOS_RECURSO.contains(ex.codigo())) {
                throw noDisponible();
            }
            throw ex;
        }
    }

    private static void validarRango(Instant desde, Instant hasta) {
        if (!hasta.isAfter(desde) || Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new ReglaNegocioException(RANGO_INVALIDO, EstadoError.NO_PROCESABLE_422,
                    "El rango debe tener fin posterior al inicio y abarcar como máximo 31 días");
        }
    }

    /** El 404 uniforme de la reserva online: profesional y prestación activos y el profesional la realiza. */
    public void validarRecursos(Long profesionalId, Long prestacionId) {
        boolean profesionalActivo = profesionales.buscarPorId(profesionalId)
                .map(ProfesionalConsulta.Datos::activo).orElse(false);
        boolean prestacionActiva = profesionalActivo && prestaciones.buscarPorId(prestacionId)
                .map(PrestacionResumen::activa).orElse(false);
        if (!prestacionActiva || !asignaciones.realiza(profesionalId, prestacionId)) {
            throw noDisponible();
        }
    }

    private static ReglaNegocioException noDisponible() {
        return new ReglaNegocioException(RECURSO_NO_DISPONIBLE, EstadoError.NO_ENCONTRADO_404, MENSAJE_NO_DISPONIBLE);
    }
}
