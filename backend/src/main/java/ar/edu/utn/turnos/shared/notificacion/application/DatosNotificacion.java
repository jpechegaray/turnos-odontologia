package ar.edu.utn.turnos.shared.notificacion.application;

import java.time.Instant;

/**
 * Datos que aporta el consumidor para armar un email de un solo destinatario. Todos los campos
 * opcionales pueden ser nulos: la plantilla omite las líneas que faltan. Nunca incluye datos de otros
 * pacientes (RN-35).
 *
 * @param destinatario email del paciente; si es nulo o blanco no se notifica
 * @param nombrePaciente nombre del destinatario, solo para el saludo
 * @param inicio fecha y hora del turno (o del hueco) que se formatea en Buenos Aires
 * @param prestacion prestación del turno, si corresponde
 * @param profesional profesional que atiende, si corresponde
 * @param enlace enlace con token que aporta el consumidor, si corresponde
 */
public record DatosNotificacion(
        String destinatario,
        String nombrePaciente,
        Instant inicio,
        String prestacion,
        String profesional,
        String enlace) {
}
