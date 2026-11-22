package ar.edu.utn.turnos.profesionales.horario.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/** Sin {@code @Transactional}: cada hilo necesita su propia transacción real y confirmada. */
class ServicioHorariosConcurrenciaIT extends PruebaIntegracionBase {
    @Autowired
    private ServicioHorarios servicio;

    @Autowired
    private TransactionTemplate transacciones;

    @Autowired
    private JdbcTemplate jdbc;

    private Long profesional;

    @BeforeEach
    void preparar() {
        profesional = jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', 'MP-HORARIOS-CARRERA', true) returning id
                """, Long.class);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from horario_atencion where profesional_id = ?", profesional);
        jdbc.update("delete from profesional where id = ?", profesional);
    }

    @Test
    @DisplayName("Dos altas concurrentes de tramos superpuestos: una gana y la otra recibe 409")
    void altasConcurrentes() throws Exception {
        var primeraInsertada = new CountDownLatch(1);
        var liberarPrimera = new CountDownLatch(1);

        var primera = enHilo(() -> transacciones.execute(estado -> {
            var creado = servicio.crear(profesional, tramo("09:00", "13:00"));
            primeraInsertada.countDown();
            esperar(liberarPrimera);
            return creado;
        }));
        assertThat(primeraInsertada.await(30, TimeUnit.SECONDS)).isTrue();

        var segunda = enHilo(() -> servicio.crear(profesional, tramo("12:00", "14:00")));
        // La segunda queda esperando el bloqueo de fila del profesional mientras la primera no confirma.
        assertThat(terminaAntesDe(segunda, 1)).isFalse();

        liberarPrimera.countDown();
        assertThat(primera.get(30, TimeUnit.SECONDS)).isNotNull();
        assertThat(segunda).failsWithin(30, TimeUnit.SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo("HORARIO_SUPERPUESTO"));
        assertThat(jdbc.queryForObject("select count(*) from horario_atencion where profesional_id = ?",
                Long.class, profesional)).isEqualTo(1);
    }

    private static <T> CompletableFuture<T> enHilo(Supplier<T> tarea) {
        return CompletableFuture.supplyAsync(() -> {
            var autenticacion = new UsernamePasswordAuthenticationToken("recepcion-ficticia", null,
                    AuthorityUtils.createAuthorityList("ROLE_RECEPCION"));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            try {
                return tarea.get();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private static boolean terminaAntesDe(CompletableFuture<?> tarea, int segundos) throws Exception {
        try {
            tarea.get(segundos, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException ex) {
            return false;
        } catch (ExecutionException ex) {
            return true;
        }
    }

    private static void esperar(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static Tramo tramo(String inicio, String fin) {
        return new Tramo(1, LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
