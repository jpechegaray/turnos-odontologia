package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.convention.TestBean;

class AutenticacionPersonalIT extends PruebaIntegracionBase {

    private static final String CLAVE = "clave-fixture-personal";
    private static final String INCORRECTA = "clave-fixture-incorrecta";
    private static final Instant INICIO = Instant.parse("2026-11-21T12:00:00Z");
    private static final Instant VENCE = INICIO.plusSeconds(900);

    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private AutenticacionPersonal autenticacion;
    @Autowired private JdbcTemplate jdbc;
    @TestBean(methodName = "crearReloj") private Clock reloj;

    static Clock crearReloj() {
        // El inicializador de configuración consulta el reloj antes de @BeforeEach.
        Clock reloj = mock(Clock.class);
        when(reloj.instant()).thenReturn(INICIO);
        when(reloj.getZone()).thenReturn(ZonaHoraria.BUENOS_AIRES);
        return reloj;
    }

    @BeforeEach
    void fijaReloj() {
        when(reloj.instant()).thenReturn(INICIO);
    }

    @Test
    @DisplayName("El quinto fallo bloquea hasta el vencimiento exacto")
    void permiteReingresarEnElInstanteExacto() {
        String email = "vence-exacto@consultorio.test";
        bloquear(email);

        when(reloj.instant()).thenReturn(VENCE.minusMillis(1));
        assertThat(autenticacion.autenticar(email, CLAVE)).isEmpty();
        assertThat(fallos(email)).isEqualTo(5);

        when(reloj.instant()).thenReturn(VENCE);
        assertThat(autenticacion.autenticar(email, CLAVE)).isPresent();
        assertThat(fallos(email)).isZero();
    }

    @Test
    @DisplayName("Después del vencimiento se inicia una nueva serie")
    void primerFalloTrasElVencimientoNoVuelveABloquear() {
        String email = "nueva-serie@consultorio.test";
        bloquear(email);

        when(reloj.instant()).thenReturn(VENCE);
        assertThat(autenticacion.autenticar(email, INCORRECTA)).isEmpty();
        assertThat(fallos(email)).isEqualTo(1);
        assertThat(autenticacion.autenticar(email, CLAVE)).isPresent();
        assertThat(fallos(email)).isZero();
    }

    @Test
    @DisplayName("Un fallo de credenciales queda registrado y un éxito reinicia el contador")
    void persisteFallosYReinicia() {
        String email = "registro@consultorio.test";
        guardar(email);

        assertThat(autenticacion.autenticar(email, INCORRECTA)).isEmpty();
        assertThat(fallos(email)).isEqualTo(1);
        assertThat(autenticacion.autenticar("REGISTRO@consultorio.test", CLAVE))
                .hasValueSatisfying(identidad -> assertThat(identidad.rol()).isEqualTo(Rol.RECEPCION));
        assertThat(fallos(email)).isZero();
    }

    @Test
    @DisplayName("Los fallos concurrentes no se pierden y el quinto bloquea la cuenta")
    void persisteFallosConcurrentes() throws Exception {
        String email = "concurrencia@consultorio.test";
        guardar(email);
        for (int i = 0; i < 3; i++) {
            assertThat(autenticacion.autenticar(email, INCORRECTA)).isEmpty();
        }
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            Callable<Boolean> fallo = () -> {
                inicio.await();
                return autenticacion.autenticar(email, INCORRECTA).isEmpty();
            };
            var primero = ejecutor.submit(fallo);
            var segundo = ejecutor.submit(fallo);
            inicio.countDown();
            assertThat(primero.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(segundo.get(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(fallos(email)).isEqualTo(5);
        assertThat(autenticacion.autenticar(email, CLAVE)).isEmpty();
    }

    @Test
    @DisplayName("Una cuenta inactiva no puede iniciar sesión")
    void rechazaCuentaInactiva() {
        String email = "inactivo@consultorio.test";
        guardar(email);
        jdbc.update("update usuario set activo = false where email = ?", email);

        assertThat(autenticacion.autenticar(email, CLAVE)).isEmpty();
        assertThat(fallos(email)).isZero();
    }

    private void guardar(String email) {
        usuarios.saveAndFlush(new Usuario(email, new BCryptPasswordEncoder().encode(CLAVE),
                "Personal Ficticio", Rol.RECEPCION, Instant.parse("2026-11-21T12:00:00Z")));
    }

    private void bloquear(String email) {
        guardar(email);
        for (int i = 0; i < 5; i++) {
            assertThat(autenticacion.autenticar(email, INCORRECTA)).isEmpty();
        }
        assertThat(fallos(email)).isEqualTo(5);
    }

    private int fallos(String email) {
        return jdbc.queryForObject("select intentos_fallidos from usuario where email = ?", Integer.class, email);
    }
}
