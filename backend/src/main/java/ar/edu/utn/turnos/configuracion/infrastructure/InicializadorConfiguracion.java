package ar.edu.utn.turnos.configuracion.infrastructure;

import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import java.time.Clock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorConfiguracion {

    private final ConfiguracionRepositorio repositorio;
    private final Clock reloj;

    public InicializadorConfiguracion(ConfiguracionRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void inicializar() {
        var ahora = reloj.instant();
        for (var parametro : ParametroConfiguracion.values()) {
            if (!repositorio.existsById(parametro.clave())) {
                repositorio.save(new Configuracion(parametro, ahora));
            }
        }
    }
}
