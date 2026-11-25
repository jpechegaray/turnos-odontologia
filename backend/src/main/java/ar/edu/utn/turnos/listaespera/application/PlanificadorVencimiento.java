package ar.edu.utn.turnos.listaespera.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Vence una vez por día las entradas con fecha de fin pasada (RN-21). Es la primera tarea programada
 * del proyecto: habilita {@code @EnableScheduling}. El cron se puede cambiar con
 * {@code lista-espera.vencimiento.cron}; el perfil {@code test} lo desactiva con {@code -}.
 */
@Configuration
@EnableScheduling
public class PlanificadorVencimiento {
    private final ServicioListaEspera servicio;

    public PlanificadorVencimiento(ServicioListaEspera servicio) {
        this.servicio = servicio;
    }

    @Scheduled(cron = "${lista-espera.vencimiento.cron:0 5 0 * * *}", zone = "America/Argentina/Buenos_Aires")
    public void ejecutar() {
        servicio.vencerEntradas();
    }
}
