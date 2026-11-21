package ar.edu.utn.turnos.configuracion.application;

import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import ar.edu.utn.turnos.configuracion.infrastructure.ConfiguracionRepositorio;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReemplazarConfiguracion {

    private final ConfiguracionRepositorio repositorio;
    private final Clock reloj;

    public ReemplazarConfiguracion(ConfiguracionRepositorio repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public Map<String, Object> ejecutar(Map<ParametroConfiguracion, ?> valores) {
        if (!valores.keySet().equals(Set.of(ParametroConfiguracion.values()))) {
            throw new IllegalArgumentException("El reemplazo requiere todos los parámetros de configuración");
        }

        Map<String, Object> porClave = new LinkedHashMap<>();
        for (var parametro : ParametroConfiguracion.values()) {
            porClave.put(parametro.clave(), parametro.validar(valores.get(parametro)));
        }
        var filas = repositorio.bloquearParaReemplazo(List.copyOf(porClave.keySet()));
        if (filas.size() != porClave.size()) {
            throw new IllegalStateException("Faltan parámetros obligatorios de configuración");
        }

        var ahora = reloj.instant();
        for (var fila : filas) {
            fila.actualizar(porClave.get(fila.clave()).toString(), ahora);
        }
        return porClave;
    }
}
