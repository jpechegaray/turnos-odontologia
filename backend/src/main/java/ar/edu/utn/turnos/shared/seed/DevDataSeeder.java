package ar.edu.utn.turnos.shared.seed;

import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.pacientes.application.PacienteServicio;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.prestaciones.infrastructure.CatalogoPrestaciones;
import ar.edu.utn.turnos.profesionales.horario.application.ServicioHorarios;
import ar.edu.utn.turnos.profesionales.horario.application.Tramo;
import ar.edu.utn.turnos.profesionales.prestacion.application.ServicioProfesionalPrestaciones;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.profesionales.sillon.application.ServicioSillones;
import ar.edu.utn.turnos.seguridad.domain.ClaveUsuario;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.seguridad.infrastructure.GestionUsuarios;
import ar.edu.utn.turnos.shared.tiempo.ZonaHoraria;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("dev")
class DevDataSeeder implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String ADMIN_DESARROLLO = "admin@consultorio.test";
    // Namespace bigint propio, distinto del bloqueo de agenda con dos claves int.
    private static final long BLOQUEO_SEED = 33L;
    private static final LocalTime PRIMER_TURNO = LocalTime.of(10, 0);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final ServicioProfesionales profesionales;
    private final ServicioSillones sillones;
    private final CatalogoPrestaciones prestaciones;
    private final ServicioProfesionalPrestaciones asignaciones;
    private final ServicioHorarios horarios;
    private final PacienteServicio pacientes;
    private final GestionUsuarios usuarios;
    private final CrearTurnoService turnos;
    private final Clock reloj;
    private final String clavePersonal;

    DevDataSeeder(JdbcTemplate jdbc, PlatformTransactionManager transacciones, ServicioProfesionales profesionales,
            ServicioSillones sillones, CatalogoPrestaciones prestaciones, ServicioProfesionalPrestaciones asignaciones,
            ServicioHorarios horarios, PacienteServicio pacientes, GestionUsuarios usuarios, CrearTurnoService turnos,
            Clock reloj, @Value("${SEED_STAFF_PASSWORD:}") String clavePersonal) {
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transacciones);
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.prestaciones = prestaciones;
        this.asignaciones = asignaciones;
        this.horarios = horarios;
        this.pacientes = pacientes;
        this.usuarios = usuarios;
        this.turnos = turnos;
        this.reloj = reloj;
        this.clavePersonal = clavePersonal;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (clavePersonal.isBlank()) {
            return;
        }
        transaccion.executeWithoutResult(estado -> {
            jdbc.queryForObject("select cast(pg_advisory_xact_lock(?) as text)", String.class, BLOQUEO_SEED);
            if (hayDatosOperativos()) {
                LOG.info("Se omitió la carga de demostración porque la base ya contiene datos operativos");
                return;
            }
            if (!ClaveUsuario.esValida(clavePersonal)) {
                throw new IllegalStateException("SEED_STAFF_PASSWORD debe tener al menos 8 caracteres y hasta 72 bytes UTF-8");
            }
            cargarComoAdministradorInterno();
        });
    }

    private boolean hayDatosOperativos() {
        // También detecta filas inactivas y permite el administrador inicial, cualquiera sea el orden de los runners.
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from usuario where email <> ? or rol <> 'ADMIN')
                    or exists (select 1 from profesional)
                    or exists (select 1 from sillon)
                    or exists (select 1 from prestacion)
                    or exists (select 1 from pacientes)
                    or exists (select 1 from turno)
                """, Boolean.class, ADMIN_DESARROLLO));
    }

    private void cargarComoAdministradorInterno() {
        // Contexto del hilo de arranque para reutilizar las validaciones y permisos de los servicios.
        var previo = SecurityContextHolder.getContext();
        var interno = SecurityContextHolder.createEmptyContext();
        interno.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "seed-desarrollo", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(interno);
        try {
            cargarDatos();
        } finally {
            SecurityContextHolder.setContext(previo);
        }
    }

    private void cargarDatos() {
        var ana = profesionales.crear("Ana", "Ficticia", "MP-0001", "Odontología general").id();
        var bruno = profesionales.crear("Bruno", "Inventado", "MP-0002", "Ortodoncia").id();
        var recepcion = usuarios.crear("recepcion@consultorio.test", "Recepción ficticia",
                Rol.RECEPCION, clavePersonal, null).id();
        usuarios.crear("odonto1@consultorio.test", "Ana Ficticia", Rol.ODONTOLOGO, clavePersonal, ana);
        usuarios.crear("odonto2@consultorio.test", "Bruno Inventado", Rol.ODONTOLOGO, clavePersonal, bruno);

        var primero = sillones.crear("Sillón 1", null).id();
        var segundo = sillones.crear("Sillón 2", null).id();
        sillones.crear("Sillón 3", null);

        var consulta = prestaciones.crear("Consulta", 30).id();
        var limpieza = prestaciones.crear("Limpieza", 45).id();
        var extraccion = prestaciones.crear("Extracción simple", 30).id();
        var conducto = prestaciones.crear("Tratamiento de conducto", 90).id();
        var control = prestaciones.crear("Control de ortodoncia", 20).id();
        asignaciones.reemplazar(ana, List.of(consulta, limpieza, extraccion, conducto));
        asignaciones.reemplazar(bruno, List.of(control));
        for (var profesional : List.of(ana, bruno)) {
            for (int dia = 1; dia <= 5; dia++) {
                horarios.crear(profesional, new Tramo(dia, LocalTime.of(9, 0), LocalTime.of(13, 0)));
                horarios.crear(profesional, new Tramo(dia, LocalTime.of(15, 0), LocalTime.of(19, 0)));
            }
        }
        var uno = crearPaciente(1, "Prueba Uno");
        var dos = crearPaciente(2, "Prueba Dos");
        var tres = crearPaciente(3, "Prueba Tres");

        var fecha = primerDiaDeDemostracion();
        crearTurno(uno, ana, primero, consulta, fecha, PRIMER_TURNO, recepcion);
        crearTurno(dos, ana, primero, consulta, fecha, LocalTime.of(10, 30), recepcion);
        crearTurno(tres, bruno, segundo, control, fecha, LocalTime.of(11, 0), recepcion);
    }

    private Long crearPaciente(int numero, String apellido) {
        return pacientes.crear(Paciente.nuevo("Paciente", apellido, "0000000" + numero,
                "paciente" + numero + "@example.test", "0000-00000" + numero, "Obra Social Ficticia", null, null)).id();
    }

    private LocalDate primerDiaDeDemostracion() {
        Instant ahora = reloj.instant();
        LocalDate fecha = ahora.atZone(ZonaHoraria.BUENOS_AIRES).toLocalDate();
        while (fecha.getDayOfWeek().getValue() >= DayOfWeek.SATURDAY.getValue()
                || !inicio(fecha, PRIMER_TURNO).isAfter(ahora)) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private void crearTurno(Long paciente, Long profesional, Long sillon, Long prestacion, LocalDate fecha,
            LocalTime hora, Long recepcion) {
        turnos.crear(new SolicitudTurno(paciente, profesional, sillon, prestacion, inicio(fecha, hora), null),
                "Turno ficticio de demostración", recepcion);
    }

    private static Instant inicio(LocalDate fecha, LocalTime hora) {
        return fecha.atTime(hora).atZone(ZonaHoraria.BUENOS_AIRES).toInstant();
    }
}
