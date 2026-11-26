package ar.edu.utn.turnos.shared.notificacion.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reintento del email {@code CONFIRMACION_RESERVA} (C-42, spec notificaciones-email; RN-30): se rearma con un token
 * nuevo del turno y se descarta si el turno ya no está vigente. Reloj fijo: martes 1/12/2026 09:00 de Buenos Aires.
 */
@Import(RelojFijoConfig.class)
class ReintentoConfirmacionReservaIT extends PruebaIntegracionBase {
    private static final Instant INICIO = Instant.parse("2026-12-04T12:00:00Z");
    private static final Pattern ENLACE = Pattern.compile("/turno/([A-Za-z0-9_-]{43})");

    @Autowired JdbcTemplate jdbc;
    @Autowired InMemoryNotificacionAdapter correo;
    @Autowired ServicioNotificaciones notificaciones;
    @Autowired ServicioTokenTurno tokens;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long turno;

    @BeforeEach
    void preparar() {
        correo.limpiar();
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, INICIO.toString(),
                INICIO.plusSeconds(1800).toString(), "PROGRAMADO");
        jdbc.update("update turno set canal = 'ONLINE' where id = ?", turno);
        var email = jdbc.queryForObject("select email from pacientes where id = ?", String.class, paciente);
        correo.fallarProximos(1);
        notificaciones.registrarYEnviar(TipoNotificacion.CONFIRMACION_RESERVA, turno, null, paciente,
                new DatosNotificacion(email, "Ana Ficticia", INICIO, "Consulta", "Luis Ficticio",
                        "http://localhost:5173/turno/" + tokens.emitir(turno).claro()));
        // El primer envío falló; se adelanta el próximo intento para que el planificador lo tome ya.
        jdbc.update("update notificacion_enviada set programada_para = ? where turno_id = ?",
                Timestamp.from(RelojFijoConfig.INSTANTE_FIJO.minusSeconds(60)), turno);
        correo.limpiar();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from notificacion_enviada where turno_id = ?", turno);
        jdbc.update("delete from token_turno where turno_id = ?", turno);
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        correo.limpiar();
    }

    private String estado() {
        return jdbc.queryForObject("select estado from notificacion_enviada where turno_id = ?", String.class, turno);
    }

    @Test
    @DisplayName("El reintento lleva un enlace válido")
    void reintentoConEnlace() {
        assertThat(estado()).isEqualTo("FALLIDA");

        notificaciones.procesarVencidas();

        assertThat(estado()).isEqualTo("ENVIADA");
        assertThat(correo.enviados()).hasSize(1);
        var enlace = ENLACE.matcher(correo.enviados().get(0).cuerpo());
        assertThat(enlace.find()).as("el email lleva el enlace del turno").isTrue();
        assertThat(tokens.validar(enlace.group(1))).isEqualTo(turno);
    }

    @Test
    @DisplayName("No se reintenta la confirmación de un turno cancelado")
    void turnoCancelado() {
        jdbc.update("update turno set estado = 'CANCELADO' where id = ?", turno);

        notificaciones.procesarVencidas();

        assertThat(correo.enviados()).isEmpty();
        assertThat(estado()).isEqualTo("FALLIDA");
        assertThat(jdbc.queryForObject("select programada_para from notificacion_enviada where turno_id = ?",
                Timestamp.class, turno)).isNull();
    }
}
