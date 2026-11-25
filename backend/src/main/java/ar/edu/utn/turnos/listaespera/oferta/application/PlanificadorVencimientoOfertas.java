package ar.edu.utn.turnos.listaespera.oferta.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vence las ofertas cuyo hueco ya empezó (RN-24, RN-38), cada {@code app.listaespera.ofertas.vencimiento.intervalo}
 * (1 minuto por defecto). La búsqueda por token ya rechaza una oferta vencida aunque este planificador no haya
 * corrido; acá solo se ordenan los estados. El perfil {@code test} lo apaga y las pruebas lo invocan con reloj fijo.
 */
@Component
@ConditionalOnProperty(prefix = "app.listaespera.ofertas.vencimiento", name = "habilitado", havingValue = "true",
        matchIfMissing = true)
class PlanificadorVencimientoOfertas {
    private final OfertasHueco ofertas;

    PlanificadorVencimientoOfertas(OfertasHueco ofertas) {
        this.ofertas = ofertas;
    }

    @Scheduled(fixedDelayString = "${app.listaespera.ofertas.vencimiento.intervalo:1m}")
    void vencer() {
        ofertas.vencerOfertas();
    }
}
