package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaVistaPrevia {
    private final NotificacionRepositorio notificaciones;
    private final TurnosPersistencia turnos;
    private final ListaEsperaPersistencia entradas;
    private final OfertasHuecoPersistencia ofertas;
    private final DatosAvisosAgendamiento datos;
    private final PlantillaNotificacion plantilla;

    ConsultaVistaPrevia(NotificacionRepositorio notificaciones, TurnosPersistencia turnos, ListaEsperaPersistencia entradas,
            OfertasHuecoPersistencia ofertas, DatosAvisosAgendamiento datos, PlantillaNotificacion plantilla) {
        this.notificaciones = notificaciones;
        this.turnos = turnos;
        this.entradas = entradas;
        this.ofertas = ofertas;
        this.datos = datos;
        this.plantilla = plantilla;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
    @Transactional(readOnly = true)
    public Optional<MensajeNotificacion> consultar(Long id) {
        return notificaciones.porId(id).map(aviso -> {
            var detalle = datosPara(aviso).orElseGet(() -> new DatosNotificacion(aviso.destinatario(), null,
                    aviso.inicioReferencia(), null, null, null));
            return plantilla.armar(aviso.tipo(), detalle);
        });
    }

    private Optional<DatosNotificacion> datosPara(NotificacionEnviada aviso) {
        if (aviso.turnoId() != null) {
            return turnos.buscar(aviso.turnoId()).filter(t -> t.pacienteId().equals(aviso.pacienteId()))
                    .flatMap(turno -> datos.paraTurno(turno, aviso.inicioReferencia()));
        }
        if (aviso.entradaListaEsperaId() != null) {
            return entradas.buscar(aviso.entradaListaEsperaId()).filter(e -> e.pacienteId().equals(aviso.pacienteId()))
                    .flatMap(datos::paraEntrada);
        }
        if (aviso.ofertaHuecoId() != null) {
            return ofertas.buscar(aviso.ofertaHuecoId()).flatMap(oferta -> entradas.buscar(oferta.listaEsperaId())
                    .filter(e -> e.pacienteId().equals(aviso.pacienteId()))
                    .flatMap(e -> datos.paraHorario(e.pacienteId(), e.prestacionId(), oferta.profesionalId(), aviso.inicioReferencia())));
        }
        return Optional.empty();
    }
}
