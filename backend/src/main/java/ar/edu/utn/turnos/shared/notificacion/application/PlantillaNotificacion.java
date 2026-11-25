package ar.edu.utn.turnos.shared.notificacion.application;

import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Arma asunto y cuerpo en texto plano (sin Thymeleaf) para un solo destinatario. El nombre del
 * consultorio se lee de {@link ConfiguracionServicio#nombreConsultorio()} en cada armado (sin caché) y
 * la fecha se formatea en {@link ZonaHoraria#BUENOS_AIRES}. No consulta otras fichas: el cuerpo nunca
 * contiene datos de otro paciente (RN-31, RN-35).
 */
@Component
public class PlantillaNotificacion {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR"))
            .withZone(ZonaHoraria.BUENOS_AIRES);

    private final ConfiguracionServicio configuracion;

    public PlantillaNotificacion(ConfiguracionServicio configuracion) {
        this.configuracion = configuracion;
    }

    public MensajeNotificacion armar(TipoNotificacion tipo, DatosNotificacion datos) {
        String consultorio = configuracion.nombreConsultorio();
        return new MensajeNotificacion(
                datos.destinatario(),
                asunto(tipo, consultorio),
                cuerpo(tipo, datos, consultorio));
    }

    private static String asunto(TipoNotificacion tipo, String consultorio) {
        String base = switch (tipo) {
            case RECORDATORIO -> "Recordatorio de turno";
            case CONFIRMACION_RESERVA -> "Confirmación de reserva";
            case CANCELACION -> "Cancelación de turno";
            case REPROGRAMACION -> "Reprogramación de turno";
            case OFERTA_HUECO -> "Hueco disponible en la lista de espera";
        };
        return base + " - " + consultorio;
    }

    private static String cuerpo(TipoNotificacion tipo, DatosNotificacion datos, String consultorio) {
        StringBuilder cuerpo = new StringBuilder();
        String nombre = (datos.nombrePaciente() == null || datos.nombrePaciente().isBlank())
                ? "Hola"
                : "Hola " + datos.nombrePaciente().trim();
        cuerpo.append(nombre).append(":\n\n");
        cuerpo.append(descripcion(tipo)).append(" en ").append(consultorio).append(".");
        if (datos.inicio() != null) {
            cuerpo.append("\nFecha y hora: ").append(FORMATO_FECHA.format(datos.inicio()));
        }
        agregarSiPresente(cuerpo, "Prestación", datos.prestacion());
        agregarSiPresente(cuerpo, "Profesional", datos.profesional());
        agregarSiPresente(cuerpo, "Enlace", datos.enlace());
        cuerpo.append("\n\n").append(consultorio);
        return cuerpo.toString();
    }

    private static String descripcion(TipoNotificacion tipo) {
        return switch (tipo) {
            case RECORDATORIO -> "Le recordamos su turno";
            case CONFIRMACION_RESERVA -> "Su reserva fue confirmada";
            case CANCELACION -> "Su turno fue cancelado";
            case REPROGRAMACION -> "Su turno fue reprogramado";
            case OFERTA_HUECO -> "Se liberó un hueco en la lista de espera";
        };
    }

    private static void agregarSiPresente(StringBuilder cuerpo, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) {
            cuerpo.append("\n").append(etiqueta).append(": ").append(valor.trim());
        }
    }
}
