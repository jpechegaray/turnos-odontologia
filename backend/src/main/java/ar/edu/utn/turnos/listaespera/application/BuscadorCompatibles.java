package ar.edu.utn.turnos.listaespera.application;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.listaespera.domain.CompatibilidadListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.Hueco;
import ar.edu.utn.turnos.listaespera.domain.PrestacionParaHueco;
import ar.edu.utn.turnos.listaespera.domain.SelectorDeAvisos;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca las entradas compatibles con un hueco (RN-22) y elige a quién avisar (RN-23, SU-14). No chequea el rol:
 * el aviso automático (C-36) lo usa desde un listener sin sesión. La consulta para recepción pasa por
 * {@link ServicioListaEspera#compatibles}, que sí lo chequea.
 */
@Component
public class BuscadorCompatibles {
    private final ListaEsperaPersistencia entradas;
    private final ConsultaPrestaciones prestaciones;
    private final ProfesionalRealizaPrestacion profesionalPrestaciones;
    private final ConfiguracionServicio configuracion;
    private final Clock reloj;

    public BuscadorCompatibles(ListaEsperaPersistencia entradas, ConsultaPrestaciones prestaciones,
            ProfesionalRealizaPrestacion profesionalPrestaciones, ConfiguracionServicio configuracion, Clock reloj) {
        this.entradas = entradas;
        this.prestaciones = prestaciones;
        this.profesionalPrestaciones = profesionalPrestaciones;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    /** Todas las entradas compatibles, por antigüedad. */
    @Transactional(readOnly = true)
    public List<EntradaListaEspera> compatibles(Hueco hueco) {
        var fecha = hueco.inicio().atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate();
        Map<Long, PrestacionParaHueco> datosPrestacion = new HashMap<>();
        Map<Long, Boolean> realiza = new HashMap<>();
        return entradas.candidatas(hueco.profesionalId(), fecha).stream()
                .filter(entrada -> CompatibilidadListaEspera.esCompatible(entrada, hueco,
                        datosPrestacion.computeIfAbsent(entrada.prestacionId(), this::prestacion),
                        realiza.computeIfAbsent(entrada.prestacionId(),
                                id -> profesionalPrestaciones.realiza(hueco.profesionalId(), id))))
                .toList();
    }

    /** Las entradas a avisar: compatibles, una por paciente, hasta el tope y solo si el hueco no empezó. */
    @Transactional(readOnly = true)
    public List<EntradaListaEspera> aAvisar(Hueco hueco) {
        Instant ahora = Instant.now(reloj);
        if (!ahora.isBefore(hueco.inicio())) {
            return List.of();
        }
        return SelectorDeAvisos.seleccionar(compatibles(hueco), hueco, ahora, configuracion.maxAvisosPorHueco());
    }

    /** Una prestación borrada no debería existir (hay FK), pero si faltara se trata como inactiva. */
    private PrestacionParaHueco prestacion(Long id) {
        return prestaciones.buscarPorId(id)
                .map(p -> new PrestacionParaHueco(p.duracionDefaultMin(), p.activa()))
                .orElse(new PrestacionParaHueco(0, false));
    }
}
