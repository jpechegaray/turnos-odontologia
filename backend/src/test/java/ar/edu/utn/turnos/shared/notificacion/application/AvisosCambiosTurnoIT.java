package ar.edu.utn.turnos.shared.notificacion.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.CancelarTurnoService;
import ar.edu.utn.turnos.agenda.application.ReprogramarTurnoService;
import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.ColaRecordatorios;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(PlanificadorRecordatoriosIT.ConfiguracionReloj.class)
@WithMockUser(roles = "RECEPCION")
class AvisosCambiosTurnoIT extends PruebaIntegracionBase {
    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant INICIO = AHORA.plusSeconds(3600);
    private static final Instant NUEVO = INICIO.plusSeconds(3600);
    private static final String EMAIL = "paciente-cambio@example.test";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CancelarTurnoService cancelar;
    @Autowired private ReprogramarTurnoService reprogramar;
    @Autowired private ApplicationEventPublisher eventos;
    @Autowired private PlatformTransactionManager gestor;
    @Autowired private PlanificadorRecordatoriosIT.RelojDePrueba reloj;
    @Autowired private ServicioNotificaciones notificaciones;
    @Autowired private PlanificadorRecordatorios planificador;
    @Autowired private ColaRecordatorios cola;
    @Autowired private InMemoryNotificacionAdapter correo;

    private Long paciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;
    private Long usuario;
    private Long turno;

    @BeforeEach
    void preparar() {
        reloj.ahora = AHORA;
        correo.limpiar();
        paciente = DatosAgenda.paciente(jdbc, true);
        jdbc.update("update pacientes set email = ? where id = ?", EMAIL, paciente);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, 2, "08:00", "14:00");
        usuario = (Long) DatosAgenda.usuario(jdbc, "RECEPCION")[0];
        turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, INICIO.toString(),
                INICIO.plusSeconds(1800).toString(), "PROGRAMADO");
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from notificacion_enviada where turno_id = ?", turno);
        jdbc.update("delete from token_turno where turno_id = ?", turno);
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        jdbc.update("delete from usuario where id = ?", usuario);
        correo.limpiar();
    }

    @Test
    @DisplayName("La cancelación del personal envía un solo correo")
    void cancelacionIdempotente() {
        var cancelado = cancelar.cancelarPorPersonal(turno, null, usuario);
        new TransactionTemplate(gestor).executeWithoutResult(
                estado -> eventos.publishEvent(TurnoCancelado.de(cancelado)));

        assertThat(registro("CANCELACION", INICIO)).containsEntry("estado", "ENVIADA");
        assertThat(correo.enviados()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.destinatario()).isEqualTo(EMAIL);
            assertThat(mensaje.asunto()).startsWith("Cancelación de turno");
            assertThat(mensaje.cuerpo()).contains("01/12/2026 10:00");
        });
    }

    @Test
    @DisplayName("La reprogramación avisa el nuevo horario y descarta el anterior")
    void reprogramacionConAvisoYRecordatorioNuevo() {
        encolarRecordatorio();
        jdbc.update("""
                update notificacion_enviada set estado = 'FALLIDA', intentos = 1
                where turno_id = ? and tipo = 'RECORDATORIO'
                """, turno);
        reprogramar(NUEVO, null);

        assertThat(registro("REPROGRAMACION", NUEVO)).containsEntry("estado", "ENVIADA");
        assertQueSeDescartoRecordatorio();
        assertThat(registro("RECORDATORIO", INICIO)).containsEntry("intentos", 1);
        assertThat(correo.enviados()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.destinatario()).isEqualTo(EMAIL);
            assertThat(mensaje.asunto()).startsWith("Reprogramación de turno");
            assertThat(mensaje.cuerpo()).contains("01/12/2026 11:00");
        });
        planificador.procesar();
        assertThat(registro("RECORDATORIO", NUEVO)).containsEntry("estado", "ENVIADA");
        assertThat(correo.enviados()).hasSize(2);
    }

    @Test
    @DisplayName("Un cambio sin email descarta el recordatorio sin enviar avisos")
    void cambiosSinEmail() {
        encolarRecordatorio();
        jdbc.update("update pacientes set email = null where id = ?", paciente);
        assertThat(reprogramar(NUEVO, null).intervalo().inicio()).isEqualTo(NUEVO);
        assertQueSeDescartoRecordatorio();
        cola.encolar(turno, paciente, EMAIL, NUEVO, AHORA);
        assertThat(cancelar.cancelarPorPersonal(turno, null, usuario).estado().name()).isEqualTo("CANCELADO");

        assertThat(registro("RECORDATORIO", NUEVO)).containsEntry("estado", "FALLIDA")
                .containsEntry("programada_para", null);
        assertSinAvisos();
    }

    @Test
    @DisplayName("Una cancelación por token descarta el recordatorio sin enviar aviso")
    void cancelacionPorToken() {
        encolarRecordatorio();
        cancelar.cancelar(turno, ActorCancelacion.PACIENTE_TOKEN, null, null);

        assertQueSeDescartoRecordatorio();
        assertSinAvisos();
    }

    @Test
    @DisplayName("Una operación revertida no envía avisos ni descarta recordatorios")
    void cancelacionRevertida() {
        encolarRecordatorio();
        new TransactionTemplate(gestor).executeWithoutResult(estado -> {
            cancelar.cancelarPorPersonal(turno, null, usuario);
            estado.setRollbackOnly();
        });

        assertThat(registro("RECORDATORIO", INICIO)).containsEntry("estado", "PENDIENTE")
                .hasEntrySatisfying("programada_para", fecha -> assertThat(fecha).isNotNull());
        assertSinAvisos();
    }

    @Test
    @DisplayName("Un aviso fallido se reintenta con la misma fecha")
    void reintento() {
        correo.fallarProximos(1);
        cancelar.cancelarPorPersonal(turno, null, usuario);
        assertThat(registro("CANCELACION", INICIO)).containsEntry("estado", "FALLIDA")
                .containsEntry("intentos", 1);

        reloj.ahora = AHORA.plusSeconds(300);
        notificaciones.procesarVencidas();
        assertThat(registro("CANCELACION", INICIO)).containsEntry("estado", "ENVIADA")
                .containsEntry("intentos", 2);
        assertThat(correo.enviados()).singleElement().satisfies(mensaje -> {
            assertThat(mensaje.destinatario()).isEqualTo(EMAIL);
            assertThat(mensaje.cuerpo()).contains("01/12/2026 10:00");
        });
    }

    @Test
    @DisplayName("Una reprogramación con el mismo inicio conserva su recordatorio")
    void conservaRecordatorioConMismoInicio() {
        encolarRecordatorio();
        reprogramar(INICIO, 45);

        assertThat(registro("REPROGRAMACION", INICIO)).containsEntry("estado", "ENVIADA");
        assertThat(registro("RECORDATORIO", INICIO)).containsEntry("estado", "PENDIENTE")
                .hasEntrySatisfying("programada_para", fecha -> assertThat(fecha).isNotNull());
    }

    @Test
    @DisplayName("Volver al inicio anterior permite retomar su recordatorio descartado")
    void volverAlHorarioInicial() {
        encolarRecordatorio();
        reprogramar(NUEVO, null);
        assertQueSeDescartoRecordatorio();
        reprogramar(INICIO, null);
        planificador.procesar();

        assertThat(registro("RECORDATORIO", INICIO)).containsEntry("estado", "ENVIADA")
                .containsEntry("intentos", 1);
        assertThat(correo.enviados()).filteredOn(mensaje -> mensaje.asunto().startsWith("Recordatorio"))
                .hasSize(1);
    }

    private Turno reprogramar(Instant inicio, Integer minutos) {
        return reprogramar.reprogramar(turno, new ReprogramarTurnoService.Pedido(inicio, null, null, minutos));
    }

    private void encolarRecordatorio() {
        cola.encolar(turno, paciente, EMAIL, INICIO, AHORA);
    }

    private Map<String, Object> registro(String tipo, Instant inicio) {
        return jdbc.queryForMap("""
                select estado, intentos, programada_para from notificacion_enviada
                where turno_id = ? and tipo = ? and inicio_referencia = ?
                """, turno, tipo, inicio.atOffset(ZoneOffset.UTC));
    }

    private void assertQueSeDescartoRecordatorio() {
        assertThat(registro("RECORDATORIO", INICIO)).containsEntry("estado", "FALLIDA")
                .containsEntry("programada_para", null);
    }

    private void assertSinAvisos() {
        assertThat(jdbc.queryForObject("""
                select count(*) from notificacion_enviada where turno_id = ? and tipo <> 'RECORDATORIO'
                """, Long.class, turno)).isZero();
        assertThat(correo.intentos()).isZero();
    }
}
