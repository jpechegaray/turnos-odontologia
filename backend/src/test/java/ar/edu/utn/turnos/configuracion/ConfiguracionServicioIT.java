package ar.edu.utn.turnos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.configuracion.application.ReemplazarConfiguracion;
import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class ConfiguracionServicioIT extends PruebaIntegracionBase {

    @Autowired
    private ConfiguracionServicio servicio;

    @Autowired
    private ReemplazarConfiguracion reemplazar;

    @Test
    @Transactional
    @DisplayName("El consumidor lee valores nuevos sin alterar su resultado anterior")
    void leeConfiguracionVigente() {
        var consumidor = new ConsumidorPrueba(servicio);
        consumidor.registrar("anterior", 20);
        assertThat(consumidor.registrada("anterior")).isTrue();
        assertThat(leerParametros()).containsExactly(24, 24, 60, 2, 3, 10, "Consultorio Ficticio");

        reemplazar.ejecutar(Map.of(
                ParametroConfiguracion.PLAZO_CANCELACION, 12,
                ParametroConfiguracion.ANTICIPACION_RECORDATORIO, 24,
                ParametroConfiguracion.HORIZONTE_RESERVA, 60,
                ParametroConfiguracion.ANTICIPACION_RESERVA, 2,
                ParametroConfiguracion.TOPE_TURNOS_EMAIL, 3,
                ParametroConfiguracion.TOPE_AVISOS, 10,
                ParametroConfiguracion.NOMBRE_CONSULTORIO, "Consultorio de Prueba"));

        consumidor.registrar("posterior", 20);
        assertThat(consumidor.registrada("posterior")).isFalse();
        assertThat(leerParametros()).containsExactly(12, 24, 60, 2, 3, 10, "Consultorio de Prueba");
        assertThat(consumidor.registrada("anterior")).isTrue();
    }

    private List<Object> leerParametros() {
        return List.of(servicio.plazoCancelacionHoras(), servicio.anticipacionRecordatorioHoras(),
                servicio.horizonteReservaOnlineDias(), servicio.anticipacionMinimaReservaOnlineHoras(),
                servicio.maxTurnosActivosPorEmail(), servicio.maxAvisosPorHueco(), servicio.nombreConsultorio());
    }

    private record ConsumidorPrueba(ConfiguracionServicio servicio, Map<String, Boolean> decisiones) {

        ConsumidorPrueba(ConfiguracionServicio servicio) {
            this(servicio, new HashMap<>());
        }

        void registrar(String operacion, int horasRestantes) {
            decisiones.put(operacion, horasRestantes < servicio.plazoCancelacionHoras());
        }

        boolean registrada(String operacion) {
            return decisiones.get(operacion);
        }
    }
}
