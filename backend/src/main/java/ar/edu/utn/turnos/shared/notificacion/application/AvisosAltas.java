package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.agenda.infrastructure.TurnosPersistencia;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AvisosAltas {
    private final ListaEsperaPersistencia entradas;
    private final TurnosPersistencia turnos;
    private final DatosAvisosAgendamiento datos;
    private final ServicioNotificaciones notificaciones;

    AvisosAltas(ListaEsperaPersistencia entradas, TurnosPersistencia turnos, DatosAvisosAgendamiento datos,
            ServicioNotificaciones notificaciones) {
        this.entradas = entradas;
        this.turnos = turnos;
        this.datos = datos;
        this.notificaciones = notificaciones;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarEntrada(EventosAvisoAlta.EntradaRegistrada evento) {
        entradas.buscar(evento.entradaId()).ifPresent(entrada -> datos.paraEntrada(entrada).ifPresent(correo ->
                notificaciones.registrarIngresoLista(entrada.id(), entrada.pacienteId(), correo)));
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alAgendarTurno(EventosAvisoAlta.TurnoAgendado evento) {
        var tipo = evento.desdeListaDeEspera() ? TipoNotificacion.CONFIRMACION_LISTA_ESPERA : TipoNotificacion.CONFIRMACION_RESERVA;
        turnos.buscar(evento.turnoId()).ifPresent(turno -> datos.paraTurno(turno, turno.intervalo().inicio()).ifPresent(correo ->
                notificaciones.registrarYEnviar(tipo, turno.id(), null, turno.pacienteId(), correo)));
    }
}
