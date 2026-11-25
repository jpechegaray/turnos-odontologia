package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor del aviso automático de huecos (design D2): la cancelación no espera al SMTP. Con
 * {@code app.listaespera.avisos.sincronico=true} (perfil {@code test}) corre en el mismo hilo, así las pruebas
 * son deterministas.
 */
@Configuration
@EnableAsync
public class ConfiguracionAvisosHueco {
    public static final String EJECUTOR = "avisosHuecoExecutor";

    @Bean(EJECUTOR)
    TaskExecutor avisosHuecoExecutor(@Value("${app.listaespera.avisos.sincronico:false}") boolean sincronico) {
        if (sincronico) {
            return new SyncTaskExecutor();
        }
        var ejecutor = new ThreadPoolTaskExecutor();
        ejecutor.setCorePoolSize(1);
        ejecutor.setMaxPoolSize(2);
        ejecutor.setQueueCapacity(100);
        ejecutor.setThreadNamePrefix("aviso-hueco-");
        ejecutor.setWaitForTasksToCompleteOnShutdown(true);
        return ejecutor;
    }
}
