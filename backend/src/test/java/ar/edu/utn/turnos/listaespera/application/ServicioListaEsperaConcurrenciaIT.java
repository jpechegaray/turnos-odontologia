package ar.edu.utn.turnos.listaespera.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Sin {@code @Transactional}: cada hilo confirma su propia transacción. Las dos altas pasan la consulta
 * previa a la vez (ninguna ve la otra) y el índice único parcial decide cuál gana (RN-21).
 */
@Import(RelojFijoConfig.class)
class ServicioListaEsperaConcurrenciaIT extends PruebaIntegracionBase {
    @Autowired
    private ServicioListaEspera servicio;

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosFicticios.paciente(jdbc, "carrera@correo.test");
        prestacion = DatosFicticios.prestacion(jdbc, "Carrera lista");
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from lista_espera where paciente_id = ?", paciente);
        jdbc.update("delete from pacientes where id = ?", paciente);
        jdbc.update("delete from prestacion where id = ?", prestacion);
    }

    @Test
    @DisplayName("Dos altas simultáneas duplicadas: una gana y la otra recibe 409")
    void altasSimultaneas() throws Exception {
        var largada = new CountDownLatch(1);
        var resultados = List.of(enHilo(largada), enHilo(largada));
        largada.countDown();

        int creadas = 0;
        int rechazadas = 0;
        for (var resultado : resultados) {
            try {
                resultado.get(30, TimeUnit.SECONDS);
                creadas++;
            } catch (java.util.concurrent.ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOfSatisfying(ReglaNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo("ENTRADA_ACTIVA_DUPLICADA"));
                rechazadas++;
            }
        }
        assertThat(creadas).isEqualTo(1);
        assertThat(rechazadas).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from lista_espera where paciente_id = ? and estado = 'ACTIVA'",
                Long.class, paciente)).isEqualTo(1);
    }

    private CompletableFuture<Object> enHilo(CountDownLatch largada) {
        Supplier<Object> alta = () -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "recepcion-ficticia", null, AuthorityUtils.createAuthorityList("ROLE_RECEPCION")));
            try {
                largada.await(30, TimeUnit.SECONDS);
                return servicio.crear(new DatosAlta(paciente, prestacion, null, null, LocalDate.of(2026, 12, 31),
                        null, null));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        return CompletableFuture.supplyAsync(alta);
    }
}
