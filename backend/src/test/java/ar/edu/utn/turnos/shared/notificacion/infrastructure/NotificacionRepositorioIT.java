package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.notificacion.domain.EstadoNotificacion;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionEnviada;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionRepositorio;
import ar.edu.utn.turnos.shared.notificacion.domain.TipoNotificacion;
import ar.edu.utn.turnos.shared.paginacion.Pagina;
import java.time.Instant;
import java.util.List;
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

    // --- Búsqueda paginada (C-25) ---

    /** Turno 10: una ENVIADA y una FALLIDA; turno 20: una FALLIDA; turno 30: una PENDIENTE; más una oferta. */
    private List<Long> registrarVariadas() {
        Instant base = Instant.parse("2026-12-10T14:30:00Z");
        Long enviada = repositorio.guardar(avisoDeTurno(10L, base)
                .conEnvioExitoso(Instant.parse("2026-12-09T14:30:00Z"))).id();
        Long fallidaTurno10 = repositorio.guardar(NotificacionEnviada.pendiente(TipoNotificacion.CONFIRMACION_RESERVA,
                "paciente@example.test", 10L, null, 5L, base, base).conIntentoFallido("error", null)).id();
        Long fallidaTurno20 = repositorio.guardar(avisoDeTurno(20L, base).conIntentoFallido("error", null)).id();
        Long pendiente = repositorio.guardar(avisoDeTurno(30L, base)).id();
        Long oferta = repositorio.guardar(avisoDeOferta(100L, base)).id();
        return List.of(enviada, fallidaTurno10, fallidaTurno20, pendiente, oferta);
    }

    private static List<Long> ids(Pagina<NotificacionEnviada> pagina) {
        return pagina.contenido().stream().map(NotificacionEnviada::id).toList();
    }

    @Test
    @DisplayName("Sin filtros la búsqueda devuelve todas, de la más reciente a la más antigua")
    void buscarSinFiltros() {
        List<Long> registradas = registrarVariadas();

        Pagina<NotificacionEnviada> pagina = repositorio.buscar(null, null, 0, 20);

        assertThat(ids(pagina)).containsExactlyElementsOf(registradas.reversed());
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
    }

    @Test
    @DisplayName("La búsqueda filtra por estado, por turno y por ambos a la vez")
    void buscarConFiltros() {
        List<Long> r = registrarVariadas();

        Pagina<NotificacionEnviada> fallidas = repositorio.buscar(EstadoNotificacion.FALLIDA, null, 0, 20);
        Pagina<NotificacionEnviada> turno10 = repositorio.buscar(null, 10L, 0, 20);
        Pagina<NotificacionEnviada> fallidasTurno10 = repositorio.buscar(EstadoNotificacion.FALLIDA, 10L, 0, 20);

        assertThat(ids(fallidas)).containsExactly(r.get(2), r.get(1));
        assertThat(fallidas.totalElementos()).isEqualTo(2);
        assertThat(ids(turno10)).containsExactly(r.get(1), r.get(0));
        assertThat(turno10.totalElementos()).isEqualTo(2);
        assertThat(ids(fallidasTurno10)).containsExactly(r.get(1));
        assertThat(fallidasTurno10.totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("La búsqueda pagina con totales filtrados y una página posterior a la última viene vacía")
    void buscarPaginado() {
        List<Long> r = registrarVariadas();

        Pagina<NotificacionEnviada> primera = repositorio.buscar(null, null, 0, 2);
        Pagina<NotificacionEnviada> tercera = repositorio.buscar(null, null, 2, 2);
        Pagina<NotificacionEnviada> posterior = repositorio.buscar(null, null, 5, 2);

        assertThat(ids(primera)).containsExactly(r.get(4), r.get(3));
        assertThat(primera.totalPaginas()).isEqualTo(3);
        assertThat(ids(tercera)).containsExactly(r.get(0));
        assertThat(posterior.contenido()).isEmpty();
        assertThat(posterior.totalElementos()).isEqualTo(5);
        assertThat(posterior.pagina()).isEqualTo(5);
        assertThat(posterior.tamanio()).isEqualTo(2);
    }
}
