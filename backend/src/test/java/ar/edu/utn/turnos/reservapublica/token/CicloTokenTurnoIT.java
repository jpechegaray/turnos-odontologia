package ar.edu.utn.turnos.reservapublica.token;

import static ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig.INSTANTE_FIJO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.TurnosParaToken;
import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCancelado;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoReprogramado;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Import(RelojFijoConfig.class)
class CicloTokenTurnoIT extends PruebaIntegracionBase {
    private static final Instant INICIO = INSTANTE_FIJO.plusSeconds(7200);
    private static final Instant CANCELACION = INSTANTE_FIJO.plusSeconds(60);
    private static final ServicioToken CRIPTO = new ServicioToken();

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ServicioTokenTurno servicio;
    @Autowired
    private TurnosParaToken turnos;
    @Autowired
    private ApplicationEventPublisher eventos;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private TokenTurnoRepositorio tokens;

    private final List<TurnoPrueba> datos = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (var t : datos) {
            jdbc.update("delete from token_turno where turno_id = ?", t.id());
            DatosAgenda.limpiar(jdbc, t.paciente(), new Long[] {t.profesional()}, new Long[] {t.sillon()},
                    new Long[] {t.prestacion()});
        }
    }

    @Test
    @DisplayName("Cancelar revoca solo los tokens del turno cancelado")
    void cancelacion() {
        var turno = nuevoTurno();
        var otro = nuevoTurno();
        String primero = servicio.emitir(turno.id()).claro();
        String segundo = servicio.emitir(turno.id()).claro();
        String ajeno = servicio.emitir(otro.id()).claro();

        transaccion.executeWithoutResult(tx -> cancelar(turno, CANCELACION));
        assertThat(marca(primero).revocado()).isEqualTo(CANCELACION);
        assertThat(marca(segundo).revocado()).isEqualTo(CANCELACION);
        assertThat(marca(ajeno).revocado()).isNull();
        var inexistente = assertThrows(ReglaNegocioException.class,
                () -> servicio.validar(CRIPTO.generar().claro()));
        assertThat(inexistente.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
        for (String secreto : List.of(primero, segundo)) {
            var rechazo = assertThrows(ReglaNegocioException.class, () -> servicio.validar(secreto));
            assertThat(rechazo.estado()).isEqualTo(inexistente.estado());
            assertThat(rechazo.codigo()).isEqualTo(inexistente.codigo());
            assertThat(rechazo.getMessage()).isEqualTo(inexistente.getMessage());
        }
        assertThat(servicio.validar(ajeno)).isEqualTo(otro.id());

        transaccion.executeWithoutResult(tx -> publicarCancelacion(turno, CANCELACION.plusSeconds(60)));
        assertThat(marca(primero).revocado()).isEqualTo(CANCELACION);
        assertThat(marca(segundo).revocado()).isEqualTo(CANCELACION);
    }

    @ParameterizedTest
    @ValueSource(longs = {-3600, 3600})
    @DisplayName("Reprogramar conserva el secreto y cambia el instante de expiración")
    void reprogramacion(long desplazamiento) {
        var turno = nuevoTurno();
        String vigente = servicio.emitir(turno.id()).claro();
        String segundo = servicio.emitir(turno.id()).claro();
        String revocado = servicio.emitir(turno.id()).claro();
        String ajeno = servicio.emitir(nuevoTurno().id()).claro();
        jdbc.update("update token_turno set revocado_en = ? where token_hash = ?",
                Timestamp.from(CANCELACION), CRIPTO.hashear(revocado));
        Instant nuevoInicio = INICIO.plusSeconds(desplazamiento);

        transaccion.executeWithoutResult(tx -> reprogramar(turno, nuevoInicio));
        assertThat(marca(vigente)).isEqualTo(new Marca(nuevoInicio, null));
        assertThat(marca(segundo)).isEqualTo(new Marca(nuevoInicio, null));
        assertThat(marca(revocado)).isEqualTo(new Marca(INICIO, CANCELACION));
        assertThat(marca(ajeno)).isEqualTo(new Marca(INICIO, null));
        transaccion.executeWithoutResult(tx -> {
            var antes = conRelojEn(nuevoInicio.minusNanos(1000));
            var alInicio = conRelojEn(nuevoInicio);
            for (String secreto : List.of(vigente, segundo)) {
                assertThat(antes.validar(secreto)).isEqualTo(turno.id());
                assertThat(assertThrows(ReglaNegocioException.class, () -> alInicio.validar(secreto)).estado())
                        .isEqualTo(EstadoError.NO_ENCONTRADO_404);
            }
        });
        assertThat(assertThrows(ReglaNegocioException.class, () -> servicio.validar(revocado)).estado())
                .isEqualTo(EstadoError.NO_ENCONTRADO_404);
    }

    @Test
    @DisplayName("Los eventos de turnos sin token no generan credenciales")
    void sinTokens() {
        var turno = nuevoTurno();
        transaccion.executeWithoutResult(tx -> reprogramar(turno, INICIO.plusSeconds(3600)));
        transaccion.executeWithoutResult(tx -> cancelar(turno, CANCELACION));
        assertThat(jdbc.queryForObject("select count(*) from token_turno where turno_id = ?", Long.class, turno.id()))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Revertir la operación de agenda no cambia los tokens")
    void rollback(boolean cancelacion) {
        var turno = nuevoTurno();
        String secreto = servicio.emitir(turno.id()).claro();
        transaccion.executeWithoutResult(tx -> {
            if (cancelacion) {
                cancelar(turno, CANCELACION);
            } else {
                reprogramar(turno, INICIO.plusSeconds(3600));
            }
            tx.setRollbackOnly();
        });
        assertThat(marca(secreto)).isEqualTo(new Marca(INICIO, null));
        assertThat(servicio.validar(secreto)).isEqualTo(turno.id());
    }

    @Test
    @DisplayName("Una validación concurrente no pisa la revocación")
    void validacionDuranteCancelacion() throws Exception {
        var turno = nuevoTurno();
        String secreto = servicio.emitir(turno.id()).claro();
        var rechazo = assertThrows(ExecutionException.class,
                () -> duranteCambio(() -> cancelar(turno, CANCELACION), () -> servicio.validar(secreto)));
        assertThat(rechazo.getCause()).isInstanceOfSatisfying(ReglaNegocioException.class,
                regla -> assertThat(regla.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404));
        assertThat(marca(secreto)).isEqualTo(new Marca(INICIO, CANCELACION));
        assertThat(jdbc.queryForObject("select ultimo_uso_en from token_turno where turno_id = ?",
                Timestamp.class, turno.id())).isNull();
    }

    @Test
    @DisplayName("Una emisión concurrente con la reprogramación usa el inicio vigente")
    void emisionDuranteReprogramacion() throws Exception {
        var turno = nuevoTurno();
        Instant nuevoInicio = INICIO.plusSeconds(3600);
        var emitido = duranteCambio(() -> reprogramar(turno, nuevoInicio), () -> servicio.emitir(turno.id()));
        assertThat(marca(emitido.claro())).isEqualTo(new Marca(nuevoInicio, null));
    }

    /**
     * Confirma con PostgreSQL que la segunda transacción realmente espera la fila antes de liberar el cambio;
     * iniciar dos hilos sin observar el bloqueo podría convertir la prueba en una ejecución secuencial.
     */
    private <T> T duranteCambio(Runnable cambio, Supplier<T> operacion) throws Exception {
        try (var ejecutor = Executors.newSingleThreadExecutor()) {
            var pid = new CompletableFuture<Integer>();
            Future<T> resultado = transaccion.execute(tx -> {
                cambio.run();
                var concurrente = ejecutor.submit(() -> transaccion.execute(otra -> {
                    jdbc.execute("set local lock_timeout = '8s'");
                    pid.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                    return operacion.get();
                }));
                esperarBloqueo(pid);
                return concurrente;
            });
            return resultado.get(10, TimeUnit.SECONDS);
        }
    }

    private void esperarBloqueo(CompletableFuture<Integer> pid) {
        try {
            int proceso = pid.get(5, TimeUnit.SECONDS);
            int productor = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (Boolean.TRUE.equals(jdbc.queryForObject("select ? = any(pg_blocking_pids(?))",
                        Boolean.class, productor, proceso))) {
                    return;
                }
                Thread.sleep(20);
            } while (System.nanoTime() < limite);
            throw new AssertionError("La operación concurrente no esperó el bloqueo del productor");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Espera del bloqueo interrumpida", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AssertionError("No se obtuvo el proceso concurrente", e);
        }
    }

    private ServicioTokenTurno conRelojEn(Instant instante) {
        return new ServicioTokenTurno(turnos, tokens, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private void cancelar(TurnoPrueba t, Instant instante) {
        turnos.bloquearParaToken(t.id()).orElseThrow();
        jdbc.update("update turno set estado = 'CANCELADO', cancelado_en = ? where id = ?",
                Timestamp.from(instante), t.id());
        publicarCancelacion(t, instante);
    }

    private void publicarCancelacion(TurnoPrueba t, Instant instante) {
        turnos.bloquearParaToken(t.id()).orElseThrow();
        eventos.publishEvent(new TurnoCancelado(t.id(), t.paciente(), t.profesional(), t.sillon(),
                intervalo(INICIO), false, instante, ActorCancelacion.PACIENTE_TOKEN));
    }

    private void reprogramar(TurnoPrueba t, Instant nuevoInicio) {
        var anterior = turnos.bloquearParaToken(t.id()).orElseThrow().inicio();
        var nuevo = intervalo(nuevoInicio);
        jdbc.update("update turno set inicio = ?, fin = ? where id = ?", Timestamp.from(nuevo.inicio()),
                Timestamp.from(nuevo.fin()), t.id());
        eventos.publishEvent(new TurnoReprogramado(t.id(), t.paciente(), t.profesional(), t.sillon(),
                intervalo(anterior), t.profesional(), t.sillon(), nuevo, false));
    }

    private TurnoPrueba nuevoTurno() {
        Long p = DatosAgenda.paciente(jdbc, true);
        Long prof = DatosAgenda.profesional(jdbc, true);
        Long s = DatosAgenda.sillon(jdbc, true);
        Long pres = DatosAgenda.prestacion(jdbc, 30, true);
        Long id = DatosAgenda.turno(jdbc, p, prof, s, pres, INICIO.toString(), intervalo(INICIO).fin().toString(),
                "PROGRAMADO");
        var turno = new TurnoPrueba(id, p, prof, s, pres);
        datos.add(turno);
        return turno;
    }

    private Marca marca(String claro) {
        return jdbc.queryForObject("select expira_en, revocado_en from token_turno where token_hash = ?", (rs, i) -> {
            Timestamp revocado = rs.getTimestamp("revocado_en");
            return new Marca(rs.getTimestamp("expira_en").toInstant(), revocado == null ? null : revocado.toInstant());
        }, CRIPTO.hashear(claro));
    }

    private static Intervalo intervalo(Instant inicio) {
        return new Intervalo(inicio, inicio.plusSeconds(1800));
    }

    private record TurnoPrueba(Long id, Long paciente, Long profesional, Long sillon, Long prestacion) {
    }

    private record Marca(Instant expira, Instant revocado) {
    }
}
