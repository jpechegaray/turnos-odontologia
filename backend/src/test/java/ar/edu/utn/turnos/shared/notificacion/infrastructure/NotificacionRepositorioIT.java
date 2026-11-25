package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Persistencia real (PostgreSQL con Testcontainers) del registro de notificaciones.
 */
class NotificacionRepositorioIT extends PruebaIntegracionBase {

    @Autowired
    private NotificacionRepositorio repositorio;

    @Autowired
    private NotificacionEnviadaJpaRepository jpaRepository;

    @BeforeEach
    void limpiar() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("El repositorio guarda y relee una notificación con todos sus campos")
    void guardaYReleeUnaNotificacionConTodosSusCampos() {
        Instant inicio = Instant.parse("2026-12-10T14:30:00Z");
        Instant programada = Instant.parse("2026-12-09T14:30:00Z");
        NotificacionEnviada borrador = NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO,
                "paciente@example.test",
                10L,
                null,
                5L,
                inicio,
                programada);

        NotificacionEnviada guardada = repositorio.guardar(borrador);
        NotificacionEnviada leida = repositorio.porId(guardada.id()).orElseThrow();

        assertThat(leida.id()).isEqualTo(guardada.id());
        assertThat(leida.tipo()).isEqualTo(TipoNotificacion.RECORDATORIO);
        assertThat(leida.canal()).isEqualTo(NotificacionEnviada.CANAL_EMAIL);
        assertThat(leida.destinatario()).isEqualTo("paciente@example.test");
        assertThat(leida.turnoId()).isEqualTo(10L);
        assertThat(leida.ofertaHuecoId()).isNull();
        assertThat(leida.pacienteId()).isEqualTo(5L);
        assertThat(leida.inicioReferencia()).isEqualTo(inicio);
        assertThat(leida.estado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(leida.intentos()).isZero();
        assertThat(leida.ultimoError()).isNull();
        assertThat(leida.programadaPara()).isEqualTo(programada);
        assertThat(leida.enviadaEn()).isNull();
    }

    @Test
    @DisplayName("El repositorio actualiza el estado y los intentos de una notificación existente")
    void actualizaElEstadoYLosIntentosDeUnaNotificacionExistente() {
        NotificacionEnviada guardada = repositorio.guardar(NotificacionEnviada.pendiente(
                TipoNotificacion.CONFIRMACION_RESERVA,
                "paciente@example.test",
                11L,
                null,
                6L,
                Instant.parse("2026-12-10T14:30:00Z"),
                Instant.parse("2026-12-01T12:00:00Z")));

        Instant envio = Instant.parse("2026-12-01T12:00:05Z");
        repositorio.guardar(guardada.conEnvioExitoso(envio));

        NotificacionEnviada leida = repositorio.porId(guardada.id()).orElseThrow();
        assertThat(leida.estado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(leida.enviadaEn()).isEqualTo(envio);
        assertThat(leida.intentos()).isEqualTo(1);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    private static NotificacionEnviada avisoDeTurno(Long turnoId, Instant inicio) {
        return NotificacionEnviada.pendiente(
                TipoNotificacion.RECORDATORIO, "paciente@example.test", turnoId, null, 5L, inicio, inicio);
    }

    private static NotificacionEnviada avisoDeOferta(Long ofertaId, Instant inicioHueco) {
        return NotificacionEnviada.pendiente(
                TipoNotificacion.OFERTA_HUECO, "paciente@example.test", null, ofertaId, 5L, inicioHueco, inicioHueco);
    }

    @Test
    @DisplayName("Dos inserciones iguales violan la restricción única")
    void dosInsercionesIgualesViolanLaRestriccionUnica() {
        Instant inicio = Instant.parse("2026-12-10T14:30:00Z");
        repositorio.guardar(avisoDeTurno(10L, inicio));

        assertThatThrownBy(() -> repositorio.guardar(avisoDeTurno(10L, inicio)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un turno reprogramado genera un nuevo recordatorio")
    void unTurnoReprogramadoGeneraUnNuevoRecordatorio() {
        Instant inicioOriginal = Instant.parse("2026-12-10T14:30:00Z");
        Instant inicioNuevo = Instant.parse("2026-12-12T09:00:00Z");

        NotificacionEnviada original = repositorio.guardar(avisoDeTurno(10L, inicioOriginal));
        NotificacionEnviada reprogramado = repositorio.guardar(avisoDeTurno(10L, inicioNuevo));

        assertThat(reprogramado.id()).isNotEqualTo(original.id());
        assertThat(reprogramado.inicioReferencia()).isEqualTo(inicioNuevo);
        assertThat(repositorio.porId(original.id())).isPresent();
        assertThat(jpaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Los avisos de oferta no colisionan entre sí ni con los de turno")
    void losAvisosDeOfertaNoColisionanEntreSiNiConLosDeTurno() {
        Instant inicioHueco = Instant.parse("2026-12-10T14:30:00Z");

        repositorio.guardar(avisoDeOferta(100L, inicioHueco));
        repositorio.guardar(avisoDeOferta(101L, inicioHueco));
        repositorio.guardar(avisoDeTurno(10L, inicioHueco));

        assertThat(jpaRepository.count()).isEqualTo(3);

        assertThatThrownBy(() -> repositorio.guardar(avisoDeOferta(100L, inicioHueco)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> repositorio.guardar(avisoDeTurno(10L, inicioHueco)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jpaRepository.count()).isEqualTo(3);
    }
}
