package ar.edu.utn.turnos.configuracion.application;

import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import ar.edu.utn.turnos.configuracion.infrastructure.ConfiguracionRepositorio;
import ar.edu.utn.turnos.configuracion.infrastructure.ConfiguracionRepositorio.ValorVigente;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConfiguracionServicio {

    private final ConfiguracionRepositorio repositorio;

    public ConfiguracionServicio(ConfiguracionRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public int plazoCancelacionHoras() {
        return entero(ParametroConfiguracion.PLAZO_CANCELACION);
    }

    public int anticipacionRecordatorioHoras() {
        return entero(ParametroConfiguracion.ANTICIPACION_RECORDATORIO);
    }

    public int horizonteReservaOnlineDias() {
        return entero(ParametroConfiguracion.HORIZONTE_RESERVA);
    }

    public int anticipacionMinimaReservaOnlineHoras() {
        return entero(ParametroConfiguracion.ANTICIPACION_RESERVA);
    }

    public int maxTurnosActivosPorEmail() {
        return entero(ParametroConfiguracion.TOPE_TURNOS_EMAIL);
    }

    public int maxAvisosPorHueco() {
        return entero(ParametroConfiguracion.TOPE_AVISOS);
    }

    public String nombreConsultorio() {
        return valor(ParametroConfiguracion.NOMBRE_CONSULTORIO);
    }

    public String domicilioConsultorio() {
        return valor(ParametroConfiguracion.DOMICILIO_CONSULTORIO);
    }

    public String emailContactoConsultorio() {
        return valor(ParametroConfiguracion.EMAIL_CONTACTO_CONSULTORIO);
    }

    public int retencionAnios() {
        return entero(ParametroConfiguracion.RETENCION_ANIOS);
    }

    public Map<String, Object> consultar() {
        var vigentes = repositorio.consultarValores().stream()
                .collect(Collectors.toMap(ValorVigente::getClave, ValorVigente::getValor));
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (var parametro : ParametroConfiguracion.values()) {
            var valor = vigentes.get(parametro.clave());
            if (valor == null) {
                throw parametroAusente(parametro);
            }
            resultado.put(parametro.clave(), parametro.esNumerico() ? Integer.valueOf(valor) : valor);
        }
        return resultado;
    }

    private int entero(ParametroConfiguracion parametro) {
        return Integer.parseInt(valor(parametro));
    }

    private String valor(ParametroConfiguracion parametro) {
        return repositorio.consultarValor(parametro.clave()).orElseThrow(() -> parametroAusente(parametro));
    }

    private static IllegalStateException parametroAusente(ParametroConfiguracion parametro) {
        return new IllegalStateException("Falta un parámetro obligatorio de configuración: " + parametro.clave());
    }
}
