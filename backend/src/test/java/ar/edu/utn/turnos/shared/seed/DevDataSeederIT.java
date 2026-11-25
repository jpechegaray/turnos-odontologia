package ar.edu.utn.turnos.shared.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.pacientes.application.PacienteServicio;
import ar.edu.utn.turnos.prestaciones.infrastructure.CatalogoPrestaciones;
import ar.edu.utn.turnos.profesionales.horario.application.ServicioHorarios;
import ar.edu.utn.turnos.profesionales.prestacion.application.ServicioProfesionalPrestaciones;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.profesionales.sillon.application.ServicioSillones;
import ar.edu.utn.turnos.seguridad.infrastructure.GestionUsuarios;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Import(RelojFijoConfig.class)
class DevDataSeederIT extends PruebaIntegracionBase {
    private static final Clock SABADO = Clock.fixed(Instant.parse("2026-12-05T14:00:00Z"), ZoneOffset.UTC);
    private static final String CLAVE_FICTICIA = "solo-fixture-c33";
    private static final String ADMIN = "admin@consultorio.test";
    private static final List<String> TABLAS = List.of(
            "usuario", "profesional", "sillon", "prestacion", "horario_atencion", "pacientes", "turno",
            "profesional_prestacion");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transacciones;
    @Autowired private ServicioProfesionales profesionales;
    @Autowired private ServicioSillones sillones;
    @Autowired private CatalogoPrestaciones prestaciones;
    @Autowired private ServicioProfesionalPrestaciones asignaciones;
    @Autowired private ServicioHorarios horarios;
    @Autowired private PacienteServicio pacientes;
    @Autowired private GestionUsuarios usuarios;
    @Autowired private CrearTurnoService turnos;

    @Test
    @DisplayName("El seed solo se habilita en desarrollo")
    void soloDesarrollo() {
        var contexto = new ApplicationContextRunner()
                .withBean(JdbcTemplate.class, () -> jdbc)
                .withBean(PlatformTransactionManager.class, () -> transacciones)
                .withBean(ServicioProfesionales.class, () -> profesionales)
                .withBean(ServicioSillones.class, () -> sillones)
                .withBean(CatalogoPrestaciones.class, () -> prestaciones)
                .withBean(ServicioProfesionalPrestaciones.class, () -> asignaciones)
                .withBean(ServicioHorarios.class, () -> horarios)
                .withBean(PacienteServicio.class, () -> pacientes)
                .withBean(GestionUsuarios.class, () -> usuarios)
                .withBean(CrearTurnoService.class, () -> turnos)
                .withBean(Clock.class, () -> SABADO)
                .withPropertyValues("SEED_STAFF_PASSWORD=" + CLAVE_FICTICIA)
                .withUserConfiguration(DevDataSeeder.class);
        contexto.run(c -> assertThat(c).doesNotHaveBean(DevDataSeeder.class));
        contexto.withPropertyValues("spring.profiles.active=dev")
                .run(c -> assertThat(c).hasSingleBean(DevDataSeeder.class));
    }

    @Test
    @DisplayName("Sin clave de personal no se cargan datos de demostración")
    void sinClave() {
        seed(SABADO, " ", turnos).run(null);
        assertThat(cantidades()).containsOnly(0L);
    }

    @Test
    @DisplayName("Una clave de personal inválida no inicia la carga")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void claveInvalida() {
        assertThatThrownBy(() -> seed(SABADO, "breve", turnos).run(null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SEED_STAFF_PASSWORD");
        assertThat(cantidades()).containsOnly(0L);
    }

    @Test
    @DisplayName("La base vacía queda lista para demostrar la agenda")
    void cargaAgenda() {
        jdbc.update("""
                insert into usuario (email, password_hash, nombre, rol, activo, creado_en, intentos_fallidos)
                values (?, 'hash-fixture-intacto', 'Administrador ficticio', 'ADMIN', true, now(), 0)
                """, ADMIN);
        seed(SABADO, CLAVE_FICTICIA, turnos).run(null);

        assertThat(cantidades()).containsExactly(4L, 2L, 3L, 5L, 20L, 3L, 3L, 5L);
        assertThat(hashes().getFirst()).isEqualTo("hash-fixture-intacto");
        var bcrypt = new BCryptPasswordEncoder();
        assertThat(hashes().subList(1, 4)).allSatisfy(hash -> assertThat(bcrypt.matches(CLAVE_FICTICIA, hash)).isTrue());
        assertThat(jdbc.queryForList("""
                select u.email || ':' || u.rol || ':' || coalesce(p.matricula, '-')
                from usuario u left join profesional p on p.id = u.profesional_id order by u.email
                """, String.class)).containsExactly(
                ADMIN + ":ADMIN:-", "odonto1@consultorio.test:ODONTOLOGO:MP-0001",
                "odonto2@consultorio.test:ODONTOLOGO:MP-0002", "recepcion@consultorio.test:RECEPCION:-");
        assertThat(jdbc.queryForList("select nombre from sillon order by nombre", String.class))
                .containsExactly("Sillón 1", "Sillón 2", "Sillón 3");
        assertThat(jdbc.queryForList("select nombre || ':' || duracion_default_min from prestacion", String.class))
                .containsExactlyInAnyOrder("Consulta:30", "Limpieza:45", "Extracción simple:30",
                        "Tratamiento de conducto:90", "Control de ortodoncia:20");
        assertThat(jdbc.queryForList("select nombre || ' ' || apellido || ':' || dni || ':' || email from pacientes order by dni",
                String.class)).containsExactly(
                        "Paciente Prueba Uno:00000001:paciente1@example.test",
                        "Paciente Prueba Dos:00000002:paciente2@example.test",
                        "Paciente Prueba Tres:00000003:paciente3@example.test");
        assertThat(jdbc.queryForObject("""
                select count(*) from horario_atencion
                where dia_semana between 1 and 5 and (
                    (hora_inicio = time '09:00' and hora_fin = time '13:00')
                    or (hora_inicio = time '15:00' and hora_fin = time '19:00'))
                """, Long.class)).isEqualTo(20);
        assertThat(inicios()).containsExactly(Instant.parse("2026-12-07T13:00:00Z"),
                Instant.parse("2026-12-07T13:30:00Z"), Instant.parse("2026-12-07T14:00:00Z"));
        assertThat(jdbc.queryForObject("""
                select count(*) from turno a join turno b on a.fin = b.inicio
                and a.profesional_id = b.profesional_id and a.sillon_id = b.sillon_id
                """, Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Repetir la carga en otro día no duplica los datos")
    void idempotente() {
        seed(SABADO, CLAVE_FICTICIA, turnos).run(null);
        var cantidadesOriginales = cantidades();
        var horariosOriginales = inicios();
        var hashesOriginales = hashes();

        var otroDia = Clock.fixed(Instant.parse("2026-12-10T13:00:00Z"), ZoneOffset.UTC);
        seed(otroDia, "otra-clave-fixture", turnos).run(null);

        assertThat(cantidades()).isEqualTo(cantidadesOriginales);
        assertThat(inicios()).isEqualTo(horariosOriginales);
        assertThat(hashes()).isEqualTo(hashesOriginales);
    }

    @Test
    @DisplayName("Una base operativa existente se conserva")
    void conservaDatos() {
        jdbc.update("insert into sillon (nombre, activo) values ('Sillón previo ficticio', false)");
        seed(SABADO, CLAVE_FICTICIA, turnos).run(null);
        assertThat(cantidades()).containsExactly(0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L);
        assertThat(jdbc.queryForList("select nombre from sillon where not activo", String.class))
                .containsExactly("Sillón previo ficticio");
    }

    @Test
    @DisplayName("Una falla durante la carga revierte los datos")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void revierteDatosYRestauraContexto() {
        var fallanTurnos = mock(CrearTurnoService.class);
        doThrow(new IllegalStateException("Fallo de prueba")).when(fallanTurnos).crear(any(), any(), any());
        var previo = SecurityContextHolder.getContext();
        var contextoPrueba = SecurityContextHolder.createEmptyContext();
        contextoPrueba.setAuthentication(new TestingAuthenticationToken("usuario-fixture", null, "ROLE_RECEPCION"));
        SecurityContextHolder.setContext(contextoPrueba);
        try {
            assertThatThrownBy(() -> seed(SABADO, CLAVE_FICTICIA, fallanTurnos).run(null))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Fallo de prueba");
            assertThat(SecurityContextHolder.getContext()).isSameAs(contextoPrueba);
            assertThat(cantidades()).containsOnly(0L);
        } finally {
            SecurityContextHolder.setContext(previo);
        }
    }

    private DevDataSeeder seed(Clock reloj, String clave, CrearTurnoService servicioTurnos) {
        return new DevDataSeeder(jdbc, transacciones, profesionales, sillones, prestaciones, asignaciones, horarios,
                pacientes, usuarios, servicioTurnos, reloj, clave);
    }

    private List<Long> cantidades() {
        return TABLAS.stream().map(tabla -> jdbc.queryForObject("select count(*) from " + tabla, Long.class)).toList();
    }

    private List<String> hashes() {
        return jdbc.queryForList("select password_hash from usuario order by email", String.class);
    }

    private List<Instant> inicios() {
        return jdbc.query("select inicio from turno order by inicio", (rs, fila) -> rs.getTimestamp(1).toInstant());
    }
}
