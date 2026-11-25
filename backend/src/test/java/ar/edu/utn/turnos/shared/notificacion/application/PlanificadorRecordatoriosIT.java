package ar.edu.utn.turnos.shared.notificacion.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.reservapublica.token.ServicioTokenTurno;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.ColaRecordatorios;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(PlanificadorRecordatoriosIT.ConfiguracionReloj.class)
class PlanificadorRecordatoriosIT extends PruebaIntegracionBase {
    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");
    private static final Pattern ENLACE = Pattern.compile("/turno/([A-Za-z0-9_-]{43})");
    private static final String EMAIL = "ana-recordatorio@example.test";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private InMemoryNotificacionAdapter correo;
    @Autowired private PlanificadorRecordatorios planificador;
    @Autowired private ColaRecordatorios cola;
    @Autowired private ServicioTokenTurno tokens;
    @Autowired private RelojDePrueba reloj;
    private Long paciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        reloj.ahora = AHORA;
        correo.limpiar();
        paciente = DatosAgenda.paciente(jdbc, true);
        jdbc.update("update pacientes set email = ? where id = ?", EMAIL, paciente);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from notificacion_enviada where paciente_id = ?", paciente);
        jdbc.update("delete from token_turno where turno_id in (select id from turno where paciente_id = ?)", paciente);
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        correo.limpiar();
    }

    @Test
    @DisplayName("Dos ciclos no duplican el recordatorio")
    void dosCiclos() {
        var id = turno(AHORA.plusSeconds(3600));
        planificador.procesar();
        planificador.procesar();
        assertThat(correo.enviados()).hasSize(1);
        assertThat(registros(id)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Dos ciclos concurrentes encolan y envían un solo recordatorio")
    void concurrentes() throws Exception {
        var id = turno(AHORA.plusSeconds(3600));
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var primero = hilos.submit(planificador::procesar);
            var segundo = hilos.submit(planificador::procesar);
            primero.get(15, TimeUnit.SECONDS);
            segundo.get(15, TimeUnit.SECONDS);
        }
        assertThat(registros(id)).isEqualTo(1L);
        assertThat(correo.intentos()).isEqualTo(1);
    }

    @Test
    @DisplayName("La ventana configurable incluye exactamente su límite superior")
    void anticipacionConfigurable() {
        var clave = "recordatorio.anticipacion_horas";
        var anterior = jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, clave);
        try {
            jdbc.update("update configuracion set valor = '2' where clave = ?", clave);
            var limite = turno(AHORA.plusSeconds(7200));
            var fuera = turno(AHORA.plusSeconds(9000));
            planificador.procesar();
            assertThat(registros(limite)).isEqualTo(1L);
            assertThat(registros(fuera)).isZero();
            assertThat(correo.intentos()).isEqualTo(1);
        } finally {
            jdbc.update("update configuracion set valor = ? where clave = ?", anterior, clave);
        }
    }

    @Test
    @DisplayName("Un turno creado con poca anticipación entra en el siguiente ciclo")
    void altaReciente() {
        planificador.procesar();
        var id = turno(AHORA.plusSeconds(60));
        planificador.procesar();
        assertThat(registros(id)).isEqualTo(1L);
        assertThat(correo.enviados()).hasSize(1);
    }

    @Test
    @DisplayName("Un paciente sin email se omite")
    void sinEmail() {
        jdbc.update("update pacientes set email = null where id = ?", paciente);
        var id = turno(AHORA.plusSeconds(3600));
        planificador.procesar();
        assertThat(registros(id)).isZero();
        assertThat(correo.intentos()).isZero();
    }

    @Test
    @DisplayName("Un turno reprogramado recibe recordatorio para su nuevo inicio")
    void reprogramado() {
        var anterior = AHORA.plusSeconds(3600);
        var nuevo = anterior.plusSeconds(3600);
        var id = turno(anterior);
        cola.encolar(id, paciente, EMAIL, anterior, AHORA);
        jdbc.update("update turno set inicio = ?, fin = ? where id = ?",
                nuevo.atOffset(ZoneOffset.UTC), nuevo.plusSeconds(1800).atOffset(ZoneOffset.UTC), id);
        planificador.procesar();
        assertThat(registros(id)).isEqualTo(2L);
        assertThat(estado(id, anterior)).isEqualTo("FALLIDA");
        assertThat(estado(id, nuevo)).isEqualTo("ENVIADA");
        assertThat(correo.enviados()).hasSize(1);
    }

    @Test
    @DisplayName("Un turno cancelado o iniciado no recibe un pendiente")
    void canceladoOIniciado() {
        var anterior = AHORA.plusSeconds(3600);
        var nuevo = anterior.plusSeconds(3600);
        var cancelado = turno(anterior);
        var iniciado = turno(nuevo);
        cola.encolar(cancelado, paciente, EMAIL, anterior, AHORA);
        cola.encolar(iniciado, paciente, EMAIL, nuevo, AHORA);
        jdbc.update("update turno set estado = 'CANCELADO' where id = ?", cancelado);
        reloj.ahora = nuevo;
        planificador.procesar();
        assertThat(estado(cancelado, anterior)).isEqualTo("FALLIDA");
        assertThat(estado(iniciado, nuevo)).isEqualTo("FALLIDA");
        assertThat(correo.intentos()).isZero();
    }

    @Test
    @DisplayName("El recordatorio de un turno interno incluye un enlace válido")
    void enlaceParaTurnoInterno() {
        var id = turno(AHORA.plusSeconds(3600));
        var anterior = tokens.emitir(id).claro();
        planificador.procesar();
        var mensaje = correo.enviados().getFirst();
        var enlace = ENLACE.matcher(mensaje.cuerpo());
        assertThat(enlace.find()).isTrue();
        var claro = enlace.group(1);
        assertThat(tokens.validar(claro)).isEqualTo(id);
        assertThat(tokens.validar(anterior)).isEqualTo(id);
        assertThat(claro.equals(anterior)).isFalse();
        assertThat(mensaje.toString()).doesNotContain(claro);
        assertThat(jdbc.queryForObject("""
                select count(*) from token_turno where turno_id = ? and length(token_hash) = 64
                """, Long.class, id)).isEqualTo(2L);
    }

    @Test
    @DisplayName("Un fallo de correo se reintenta hasta el tope")
    void reintentos() {
        var inicio = AHORA.plusSeconds(3600);
        var id = turno(inicio);
        correo.fallarProximos(3);
        planificador.procesar();
        planificador.procesar();
        assertThat(correo.intentos()).isEqualTo(1);
        reloj.ahora = AHORA.plusSeconds(300);
        planificador.procesar();
        assertThat(correo.intentos()).isEqualTo(2);
        reloj.ahora = AHORA.plusSeconds(900);
        planificador.procesar();
        reloj.ahora = AHORA.plusSeconds(2100);
        planificador.procesar();
        assertThat(correo.intentos()).isEqualTo(3);
        assertThat(estado(id, inicio)).isEqualTo("FALLIDA");
        assertThat(jdbc.queryForObject("select count(*) from token_turno where turno_id = ?", Long.class, id))
                .isEqualTo(3L);
        assertThat(correo.enviados()).isEmpty();
    }

    private String estado(Long turnoId, Instant inicio) {
        return jdbc.queryForObject("select estado from notificacion_enviada where turno_id = ? and inicio_referencia = ?",
                String.class, turnoId, inicio.atOffset(ZoneOffset.UTC));
    }

    private Long registros(Long turnoId) {
        return jdbc.queryForObject("select count(*) from notificacion_enviada where turno_id = ?", Long.class, turnoId);
    }

    private Long turno(Instant inicio) {
        return DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plusSeconds(1800).toString(), "PROGRAMADO");
    }

    @TestConfiguration
    static class ConfiguracionReloj {
        @Bean
        @Primary
        RelojDePrueba relojDePrueba() {
            return new RelojDePrueba();
        }
    }

    static final class RelojDePrueba extends Clock {
        volatile Instant ahora = AHORA;

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZonaHoraria.BUENOS_AIRES;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(ahora, zona);
        }
    }
}
