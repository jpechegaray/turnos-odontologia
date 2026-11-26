package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ReconstructorIngresoLista implements ReconstructorDatosNotificacion {
    private final ListaEsperaPersistencia entradas;
    private final DatosAvisosAgendamiento datos;

    ReconstructorIngresoLista(ListaEsperaPersistencia entradas, DatosAvisosAgendamiento datos) {
        this.entradas = entradas;
        this.datos = datos;
    }

    @Override
    public boolean descartarSiFaltanDatos(NotificacionEnviada aviso) {
        return aviso.tipo() == TipoNotificacion.INGRESO_LISTA_ESPERA;
    }

    @Override
    public Optional<DatosNotificacion> datosPara(NotificacionEnviada aviso) {
        if (!descartarSiFaltanDatos(aviso) || aviso.entradaListaEsperaId() == null) return Optional.empty();
        return entradas.buscar(aviso.entradaListaEsperaId())
                .filter(entrada -> entrada.estado() == EstadoListaEspera.ACTIVA)
                .flatMap(datos::paraEntrada);
    }
}
