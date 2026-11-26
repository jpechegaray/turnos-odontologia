package ar.edu.utn.turnos.shared.notificacion.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Registro inmutable de una notificación enviada (KB 04, RN-30). Refleja la tabla
 * {@code notificacion_enviada}: guarda referencias por id a turno, oferta de hueco y paciente,
 * nunca entidades, para que el dominio no dependa de JPA.
 *
 * <p><b>Estados.</b> {@code PENDIENTE} es un registro creado que todavía no se envió;
 * {@code ENVIADA} es terminal y guarda {@code enviadaEn}; {@code FALLIDA} conserva
 * {@code ultimoError} y se reintenta hasta el máximo configurado. El registro se crea
 * <b>antes</b> de enviar y la idempotencia la garantiza la base con dos restricciones únicas.
 *
 * <p><b>Transiciones.</b> No hay setters: {@link #conEnvioExitoso(Instant)} y
 * {@link #conIntentoFallido(String, Instant)} devuelven copias. Cada intento real (éxito o fallo)
 * incrementa {@code intentos}; el primero incluido. La espera entre reintentos se calcula con
 * {@link #esperaCreciente(int, Duration, int)} y el instante actual lo aporta siempre el
 * {@code Clock} inyectable del servicio, nunca {@code Instant.now()}.
 */
public record NotificacionEnviada(
        Long id,
        TipoNotificacion tipo,
        String canal,
        String destinatario,
        Long turnoId,
        Long ofertaHuecoId,
        Long pacienteId,
        Instant inicioReferencia,
        EstadoNotificacion estado,
        int intentos,
        String ultimoError,
        Instant programadaPara,
        Instant enviadaEn,
        Long entradaListaEsperaId) {

    /** Único canal soportado por el MVP (KB 04). */
    public static final String CANAL_EMAIL = "EMAIL";

    public NotificacionEnviada {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de notificación es obligatorio");
        }
        if (intentos < 0) {
            throw new IllegalArgumentException("La cantidad de intentos no puede ser negativa");
        }
        canal = (canal == null || canal.isBlank()) ? CANAL_EMAIL : canal;
    }

    /**
     * Crea un registro {@code PENDIENTE} con cero intentos, listo para guardarse antes de enviar.
     *
     * @param programadaPara instante desde el cual el planificador puede enviarlo
     */
    public static NotificacionEnviada pendiente(
            TipoNotificacion tipo,
            String destinatario,
            Long turnoId,
            Long ofertaHuecoId,
            Long pacienteId,
            Instant inicioReferencia,
            Instant programadaPara) {
        return new NotificacionEnviada(
                null,
                tipo,
                CANAL_EMAIL,
                destinatario,
                turnoId,
                ofertaHuecoId,
                pacienteId,
                inicioReferencia,
                EstadoNotificacion.PENDIENTE,
                0,
                null,
                programadaPara,
                null,
                null);
    }

    public static NotificacionEnviada pendienteIngresoLista(Long entradaId, Long pacienteId, String destinatario,
            Instant programadaPara) {
        return new NotificacionEnviada(null, TipoNotificacion.INGRESO_LISTA_ESPERA, CANAL_EMAIL, destinatario,
                null, null, pacienteId, null, EstadoNotificacion.PENDIENTE, 0, null, programadaPara, null, entradaId);
    }

    /** Marca el envío como exitoso: estado {@code ENVIADA}, fecha de envío y sin último error. */
    public NotificacionEnviada conEnvioExitoso(Instant instante) {
        return new NotificacionEnviada(
                id,
                tipo,
                canal,
                destinatario,
                turnoId,
                ofertaHuecoId,
                pacienteId,
                inicioReferencia,
                EstadoNotificacion.ENVIADA,
                intentos + 1,
                null,
                programadaPara,
                instante,
                entradaListaEsperaId);
    }

    /**
     * Registra un intento fallido: estado {@code FALLIDA}, último error e instante del próximo
     * intento. Si {@code proximoIntento} es {@code null} la notificación no se vuelve a programar.
     */
    public NotificacionEnviada conIntentoFallido(String error, Instant proximoIntento) {
        return new NotificacionEnviada(
                id,
                tipo,
                canal,
                destinatario,
                turnoId,
                ofertaHuecoId,
                pacienteId,
                inicioReferencia,
                EstadoNotificacion.FALLIDA,
                intentos + 1,
                error,
                proximoIntento,
                enviadaEn,
                entradaListaEsperaId);
    }

    /** Termina un pendiente obsoleto sin contabilizar un intento de envío. */
    public NotificacionEnviada descartada(String motivo) {
        return new NotificacionEnviada(id, tipo, canal, destinatario, turnoId, ofertaHuecoId, pacienteId,
                inicioReferencia, EstadoNotificacion.FALLIDA, intentos, motivo, null, enviadaEn, entradaListaEsperaId);
    }

    public NotificacionEnviada dirigidaA(String email) {
        return new NotificacionEnviada(id, tipo, canal, email, turnoId, ofertaHuecoId, pacienteId,
                inicioReferencia, estado, intentos, ultimoError, programadaPara, enviadaEn, entradaListaEsperaId);
    }

    /** Indica si todavía se permite un nuevo intento para el máximo configurado. */
    public boolean admiteReintento(int maximoIntentos) {
        return estado != EstadoNotificacion.ENVIADA && intentos < maximoIntentos;
    }

    /**
     * Indica si el planificador debe procesar esta notificación en {@code ahora}: no está enviada,
     * su programación ya venció y, si es una oferta de hueco, el hueco todavía no comenzó (RN-38).
     */
    public boolean vencida(Instant ahora) {
        if (estado == EstadoNotificacion.ENVIADA) {
            return false;
        }
        if (programadaPara == null || programadaPara.isAfter(ahora)) {
            return false;
        }
        return tipo != TipoNotificacion.OFERTA_HUECO
                || inicioReferencia == null
                || inicioReferencia.isAfter(ahora);
    }

    /**
     * Espera antes del intento número {@code intento} (1..): {@code esperaInicial} multiplicada por
     * {@code multiplicador} elevado a {@code intento - 1}. Tras el intento 1 espera 5 min; tras el 2, 10 min.
     */
    public static Duration esperaCreciente(int intento, Duration esperaInicial, int multiplicador) {
        if (intento < 1) {
            throw new IllegalArgumentException("El número de intento debe ser al menos 1");
        }
        if (multiplicador < 1) {
            throw new IllegalArgumentException("El multiplicador debe ser al menos 1");
        }
        long factor = 1L;
        for (int i = 1; i < intento; i++) {
            factor = Math.multiplyExact(factor, multiplicador);
        }
        return esperaInicial.multipliedBy(factor);
    }
}
