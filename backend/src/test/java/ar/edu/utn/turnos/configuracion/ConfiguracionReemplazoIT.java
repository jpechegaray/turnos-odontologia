package ar.edu.utn.turnos.configuracion;

import static ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion.ANTICIPACION_RECORDATORIO;
import static ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion.PLAZO_CANCELACION;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.configuracion.application.ReemplazarConfiguracion;
import ar.edu.utn.turnos.configuracion.domain.ParametroConfiguracion;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ConfiguracionReemplazoIT extends PruebaIntegracionBase {

    @Autowired
    private ReemplazarConfiguracion reemplazar;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transacciones;

    @Autowired
    private EntityManager entidades;

    private static final String FILAS = "select clave, valor, actualizado_en from configuracion order by clave";
    private Map<ParametroConfiguracion, Object> inicial;
    private Map<ParametroConfiguracion, OffsetDateTime> fechasIniciales;

    @BeforeEach
    void guardarEstado() {
        inicial = new EnumMap<>(ParametroConfiguracion.class);
        fechasIniciales = new EnumMap<>(ParametroConfiguracion.class);
        jdbc.query(FILAS, rs -> {
            var clave = rs.getString("clave");
            var valor = rs.getString("valor");
            for (var parametro : ParametroConfiguracion.values()) {
                if (parametro.clave().equals(clave)) {
                    inicial.put(parametro, !parametro.esNumerico()
                            ? valor : Integer.valueOf(valor));
                    fechasIniciales.put(parametro, rs.getObject("actualizado_en", OffsetDateTime.class));
                }
            }
        });
    }

    @AfterEach
    void restaurarEstado() {
        for (var entrada : inicial.entrySet()) {
            jdbc.update("update configuracion set valor = ?, actualizado_en = ? where clave = ?",
                    entrada.getValue().toString(), fechasIniciales.get(entrada.getKey()), entrada.getKey().clave());
        }
    }

    @Test
    @DisplayName("El reemplazo actualiza solo las fechas de los valores modificados")
    void guardaCambios() {
        var anterior = fecha(PLAZO_CANCELACION);
        var otraFecha = fecha(ANTICIPACION_RECORDATORIO);
        var valores = new EnumMap<>(inicial);
        valores.put(PLAZO_CANCELACION, 0);

        reemplazar.ejecutar(valores);

        assertThat(valor(PLAZO_CANCELACION)).isEqualTo("0");
        assertThat(fecha(PLAZO_CANCELACION)).isNotEqualTo(anterior);
        assertThat(fecha(ANTICIPACION_RECORDATORIO)).isEqualTo(otraFecha);
    }

    @Test
    @DisplayName("El servicio conserva las fechas cuando todos los valores son iguales")
    void conservaFechasConValoresIdenticos() {
        var antes = jdbc.queryForList(FILAS);

        reemplazar.ejecutar(inicial);

        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @Test
    @DisplayName("El reemplazo inválido conserva todos los valores y fechas")
    void noGuardaCambiosValidosDeUnReemplazoInvalido() {
        var antes = jdbc.queryForList(FILAS);
        var valores = new EnumMap<>(inicial);
        valores.put(PLAZO_CANCELACION, 169);
        valores.put(ANTICIPACION_RECORDATORIO, 60);

        assertThatThrownBy(() -> reemplazar.ejecutar(valores))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> assertThat(ex.codigo()).isEqualTo("RN-36"));

        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @Test
    @DisplayName("Dos reemplazos concurrentes no mezclan valores")
    void noMezclaReemplazos() throws Exception {
        var primero = new EnumMap<>(inicial);
        primero.put(PLAZO_CANCELACION, 50);
        var segundo = new EnumMap<>(inicial);
        segundo.put(ANTICIPACION_RECORDATORIO, 72);
        var primeraEscritura = new CountDownLatch(1);
        var liberarPrimera = new CountDownLatch(1);
        var segundaIniciada = new CountDownLatch(1);

        try (var hilos = Executors.newFixedThreadPool(2)) {
            var tareaPrimera = hilos.submit(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
                reemplazar.ejecutar(primero);
                entidades.flush();
                primeraEscritura.countDown();
                esperar(liberarPrimera);
            }));
            try {
                esperar(primeraEscritura);
                var tareaSegunda = hilos.submit(() -> {
                    segundaIniciada.countDown();
                    reemplazar.ejecutar(segundo);
                });
                esperar(segundaIniciada);
                assertThatThrownBy(() -> tareaSegunda.get(300, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                liberarPrimera.countDown();
                tareaPrimera.get(10, SECONDS);
                tareaSegunda.get(10, SECONDS);
            } finally {
                liberarPrimera.countDown();
            }
        }

        assertThat(valor(PLAZO_CANCELACION)).isEqualTo(segundo.get(PLAZO_CANCELACION).toString());
        assertThat(valor(ANTICIPACION_RECORDATORIO)).isEqualTo("72");
    }

    private static void esperar(CountDownLatch senal) {
        try {
            assertThat(senal.await(10, SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private String valor(ParametroConfiguracion parametro) {
        return jdbc.queryForObject("select valor from configuracion where clave = ?", String.class, parametro.clave());
    }

    private OffsetDateTime fecha(ParametroConfiguracion parametro) {
        return jdbc.queryForObject("select actualizado_en from configuracion where clave = ?",
                (rs, fila) -> rs.getObject(1, OffsetDateTime.class), parametro.clave());
    }
}
