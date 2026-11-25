package ar.edu.utn.turnos.listaespera.oferta.application;

import ar.edu.utn.turnos.agenda.domain.evento.HuecoLiberado;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.ConfiguracionAvisosHueco;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa a la lista de espera cuando se libera un hueco (RN-06, RN-23). Escucha después del commit: si la
 * cancelación se revierte no hay aviso. Corre en su propio ejecutor y nunca propaga errores a la cancelación.
 */
@Component
class AvisoHuecoLiberado {
    private static final Logger LOG = LoggerFactory.getLogger(AvisoHuecoLiberado.class);

    private final OfertasHueco ofertas;

    AvisoHuecoLiberado(OfertasHueco ofertas) {
        this.ofertas = ofertas;
    }

    @Async(ConfiguracionAvisosHueco.EJECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alLiberarse(HuecoLiberado evento) {
        try {
            ofertas.crearYAvisar(evento.profesionalId(), evento.hueco().inicio(), evento.hueco().fin(),
                    evento.sillonId());
        } catch (RuntimeException fallo) {
            // Sin el mensaje de la excepción: podría arrastrar datos de la fila.
            LOG.error("No se pudo avisar el hueco liberado por el turno {} ({})", evento.turnoOrigenId(),
                    fallo.getClass().getSimpleName());
        }
    }
}
