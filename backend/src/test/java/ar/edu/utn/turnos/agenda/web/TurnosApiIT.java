package ar.edu.utn.turnos.agenda.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.evento.TurnoCreado;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /api/turnos} de punta a punta con PostgreSQL real (US-010, Flujo 1). El reloj fijo hace que
 * "ahora" sea el 1 de diciembre de 2026; los turnos se piden el lunes 10 de junio de 2030, cuando el
 * profesional atiende de 09:00 a 13:00 (hora de Buenos Aires, UTC-3).
 */
@Transactional
@Import(RelojFijoConfig.class)
@RecordApplicationEvents
class TurnosApiIT extends PruebaIntegracionBase {
    static final String RUTA = "/api/turnos";
    static final int LUNES = 1;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents eventos;

    @Autowired
    ar.edu.utn.turnos.agenda.application.CrearTurnoService crearTurno;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long recepcionista;
    RequestPostProcessor comoRecepcion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, LUNES, "09:00", "13:00");
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        recepcionista = (Long) usuario[0];
        comoRecepcion = sesion(recepcionista, (String) usuario[1], Rol.RECEPCION);
    }

    /** Sesión del personal con la misma identidad que deja el login (C-03). */
    static RequestPostProcessor sesion(Long usuarioId, String email, Rol rol) {
        var identidad = new IdentidadPersonal(usuarioId, email, rol);
        return authentication(new UsernamePasswordAuthenticationToken(identidad, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))));
    }

    /** Lunes 10 de junio de 2030 a la hora de Buenos Aires indicada, como instante ISO con zona. */
    static String lunes(String hora) {
        return "2030-06-10T" + hora + ":00-03:00";
    }

    String cuerpo(Long pac, Long prof, Long sil, Long pre, String inicio, Integer duracion) {
        var json = new StringBuilder("{");
        json.append("\"pacienteId\":").append(pac).append(",\"profesionalId\":").append(prof)
                .append(",\"sillonId\":").append(sil).append(",\"prestacionId\":").append(pre)
                .append(",\"inicio\":\"").append(inicio).append('"');
        if (duracion != null) {
            json.append(",\"duracionMin\":").append(duracion);
        }
        return json.append('}').toString();
    }

    String cuerpo(String hora, Integer duracion) {
        return cuerpo(paciente, profesional, sillon, prestacion, lunes(hora), duracion);
    }

    ResultActions crear(String json) throws Exception {
        return crear(json, comoRecepcion);
    }

    ResultActions crear(String json, RequestPostProcessor quien) throws Exception {
        return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json)
                .with(quien).with(csrfValido()));
    }

    int turnosDelProfesional() {
        return jdbc.queryForObject("select count(*) from turno where profesional_id = ?", Integer.class, profesional);
    }

    // --- Tarea 5.1: alta ---

    @Test
    @DisplayName("Recepción crea un turno válido")
    void recepcionCreaUnTurnoValido() throws Exception {
        var json = "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\",\"notas\":\"Control ficticio\"}"
                .formatted(paciente, profesional, sillon, prestacion, lunes("10:00"));
        var resultado = crear(json)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(".*/api/turnos/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pacienteId").value(paciente))
                .andExpect(jsonPath("$.profesionalId").value(profesional))
                .andExpect(jsonPath("$.sillonId").value(sillon))
                .andExpect(jsonPath("$.prestacionId").value(prestacion))
                .andExpect(jsonPath("$.inicio").value("2030-06-10T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2030-06-10T13:30:00Z"))
                .andExpect(jsonPath("$.duracionMin").value(30))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.canal").value("INTERNO"))
                .andExpect(jsonPath("$.notas").value("Control ficticio"))
                .andExpect(jsonPath("$.creadoEn").value(RelojFijoConfig.INSTANTE_FIJO.toString()))
                .andReturn();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"))
                .longValue();
        assertThat(jdbc.queryForObject("select creado_por_usuario_id from turno where id = ?", Long.class, id))
                .isEqualTo(recepcionista);
        assertThat(eventos.stream(TurnoCreado.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.turnoId()).isEqualTo(id);
                    assertThat(e.profesionalId()).isEqualTo(profesional);
                    assertThat(e.sillonId()).isEqualTo(sillon);
                });
    }

    @Test
    @DisplayName("Sin duración se usa la de la prestación")
    void sinDuracionSeUsaLaDeLaPrestacion() throws Exception {
        Long larga = DatosAgenda.prestacion(jdbc, 45, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, larga);
        crear(cuerpo(paciente, profesional, sillon, larga, lunes("10:00"), null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMin").value(45))
                .andExpect(jsonPath("$.fin").value("2030-06-10T13:45:00Z"));
    }

    @Test
    @DisplayName("La duración se puede ajustar")
    void laDuracionSePuedeAjustar() throws Exception {
        crear(cuerpo("10:00", 60))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMin").value(60))
                .andExpect(jsonPath("$.fin").value("2030-06-10T14:00:00Z"));
    }

    @Test
    @DisplayName("Datos obligatorios o con formato inválido")
    void datosObligatoriosOConFormatoInvalido() throws Exception {
        var sinCampo = List.of("pacienteId", "profesionalId", "sillonId", "prestacionId", "inicio");
        for (String campo : sinCampo) {
            var json = cuerpo("10:00", null).replaceFirst("\"" + campo + "\":(\"[^\"]*\"|[0-9]+),?", "")
                    .replace(",}", "}");
            crear(json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value(campo));
        }
        crear(cuerpo(paciente, profesional, sillon, prestacion, "10/06/2030 10:00", null))
                .andExpect(status().isBadRequest());
        crear(cuerpo(paciente, profesional, sillon, prestacion, "2030-06-10T10:00:00", null))
                .andExpect(status().isBadRequest());
        var notasLargas = "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\",\"notas\":\"%s\"}"
                .formatted(paciente, profesional, sillon, prestacion, lunes("10:00"), "n".repeat(1001));
        crear(notasLargas)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("notas"));
        crear("{").andExpect(status().isBadRequest());
        assertThat(turnosDelProfesional()).isZero();
    }

    // --- Tarea 5.2: recursos, tiempo y horario ---

    void rechaza422(String json, String codigo) throws Exception {
        crear(json)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    @DisplayName("Un recurso inexistente se rechaza")
    void unRecursoInexistenteSeRechaza() throws Exception {
        rechaza422(cuerpo(paciente, profesional, -1L, prestacion, lunes("10:00"), null), "SILLON_INEXISTENTE");
        rechaza422(cuerpo(-1L, profesional, sillon, prestacion, lunes("10:00"), null), "PACIENTE_INEXISTENTE");
        rechaza422(cuerpo(paciente, -1L, sillon, prestacion, lunes("10:00"), null), "PROFESIONAL_INEXISTENTE");
        rechaza422(cuerpo(paciente, profesional, sillon, -1L, lunes("10:00"), null), "PRESTACION_INEXISTENTE");
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("Un recurso inactivo se rechaza")
    void unRecursoInactivoSeRechaza() throws Exception {
        Long profesionalInactivo = DatosAgenda.profesional(jdbc, false);
        DatosAgenda.asignarPrestacion(jdbc, profesionalInactivo, prestacion);
        rechaza422(cuerpo(paciente, profesionalInactivo, sillon, prestacion, lunes("10:00"), null), "PROFESIONAL_INACTIVO");
        rechaza422(cuerpo(DatosAgenda.paciente(jdbc, false), profesional, sillon, prestacion, lunes("10:00"), null),
                "PACIENTE_INACTIVO");
        rechaza422(cuerpo(paciente, profesional, DatosAgenda.sillon(jdbc, false), prestacion, lunes("10:00"), null),
                "SILLON_INACTIVO");
        Long prestacionInactiva = DatosAgenda.prestacion(jdbc, 30, false);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacionInactiva);
        rechaza422(cuerpo(paciente, profesional, sillon, prestacionInactiva, lunes("10:00"), null), "PRESTACION_INACTIVA");
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("El profesional no realiza la prestación")
    void elProfesionalNoRealizaLaPrestacion() throws Exception {
        Long otra = DatosAgenda.prestacion(jdbc, 30, true);
        rechaza422(cuerpo(paciente, profesional, sillon, otra, lunes("10:00"), null), "RN-11");
    }

    @Test
    @DisplayName("Un turno en el pasado se rechaza")
    void unTurnoEnElPasadoSeRechaza() throws Exception {
        // "Ahora" es 2026-12-01T12:00Z (09:00 en Buenos Aires).
        rechaza422(cuerpo(paciente, profesional, sillon, prestacion, "2026-11-30T10:00:00-03:00", null), "RN-13");
        rechaza422(cuerpo(paciente, profesional, sillon, prestacion, "2026-12-01T09:00:00-03:00", null), "RN-13");
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("Un inicio fuera de la grilla de cinco minutos se rechaza")
    void unInicioFueraDeLaGrillaSeRechaza() throws Exception {
        rechaza422(cuerpo("10:03", null), "RN-09");
    }

    @Test
    @DisplayName("Una duración fuera de rango se rechaza")
    void unaDuracionFueraDeRangoSeRechaza() throws Exception {
        rechaza422(cuerpo("10:00", 7), "RN-09");
        rechaza422(cuerpo("10:00", 485), "RN-09");
        assertThat(turnosDelProfesional()).isZero();
    }

    void rechazaRn04(String json, String motivo) throws Exception {
        crear(json)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-04"))
                .andExpect(jsonPath("$.motivo").value(motivo));
    }

    @Test
    @DisplayName("Un turno que sale del horario se rechaza")
    void unTurnoQueSaleDelHorarioSeRechaza() throws Exception {
        rechazaRn04(cuerpo("12:45", 30), "FUERA_DE_HORARIO");
    }

    @Test
    @DisplayName("Un turno que termina justo al cierre del tramo se acepta")
    void unTurnoQueTerminaAlCierreSeAcepta() throws Exception {
        crear(cuerpo("12:30", 30)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Un turno que cruza el corte entre dos tramos se rechaza")
    void unTurnoQueCruzaElCorteSeRechaza() throws Exception {
        Long partido = DatosAgenda.profesional(jdbc, true);
        DatosAgenda.asignarPrestacion(jdbc, partido, prestacion);
        DatosAgenda.horario(jdbc, partido, LUNES, "09:00", "12:00");
        DatosAgenda.horario(jdbc, partido, LUNES, "14:00", "18:00");
        rechazaRn04(cuerpo(paciente, partido, sillon, prestacion, lunes("11:30"), 180), "FUERA_DE_HORARIO");
    }

    @Test
    @DisplayName("Un turno dentro de un bloqueo se rechaza")
    void unTurnoDentroDeUnBloqueoSeRechaza() throws Exception {
        DatosAgenda.bloqueo(jdbc, profesional, lunes("10:00"), lunes("11:00"));
        rechazaRn04(cuerpo("10:30", 60), "BLOQUEO");
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("Un turno que empieza cuando termina un bloqueo se acepta")
    void unTurnoQueEmpiezaCuandoTerminaUnBloqueoSeAcepta() throws Exception {
        DatosAgenda.bloqueo(jdbc, profesional, lunes("10:00"), lunes("11:00"));
        crear(cuerpo("11:00", 30)).andExpect(status().isCreated());
    }

    // --- Tarea 5.3: solapamiento ---

    void rechazaSolapado(String json, String recurso) throws Exception {
        crear(json)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"))
                .andExpect(jsonPath("$.recurso").value(recurso));
    }

    /** Profesional activo, que hace la prestación y atiende los lunes de 09:00 a 13:00. */
    Long otroProfesional() {
        Long otro = DatosAgenda.profesional(jdbc, true);
        DatosAgenda.asignarPrestacion(jdbc, otro, prestacion);
        DatosAgenda.horario(jdbc, otro, LUNES, "09:00", "13:00");
        return otro;
    }

    @Test
    @DisplayName("Un turno solapado por profesional se rechaza")
    void unTurnoSolapadoPorProfesionalSeRechaza() throws Exception {
        Long otroSillon = DatosAgenda.sillon(jdbc, true);
        crear(cuerpo("10:00", 30)).andExpect(status().isCreated());
        rechazaSolapado(cuerpo(paciente, profesional, otroSillon, prestacion, lunes("10:15"), 30), "PROFESIONAL");
        assertThat(turnosDelProfesional()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un turno solapado por sillón se rechaza")
    void unTurnoSolapadoPorSillonSeRechaza() throws Exception {
        crear(cuerpo("10:00", 30)).andExpect(status().isCreated());
        rechazaSolapado(cuerpo(paciente, otroProfesional(), sillon, prestacion, lunes("10:15"), 30), "SILLON");
    }

    @Test
    @DisplayName("Un turno que empieza justo cuando termina otro no se solapa")
    void unTurnoQueEmpiezaJustoCuandoTerminaOtroNoSeSolapa() throws Exception {
        crear(cuerpo("10:00", 30)).andExpect(status().isCreated());
        crear(cuerpo("10:30", 30)).andExpect(status().isCreated());
        assertThat(turnosDelProfesional()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un turno que empieza un minuto antes del fin de otro se solapa")
    void unTurnoQueEmpiezaUnMinutoAntesSeSolapa() throws Exception {
        // 10:29 no respeta la grilla de RN-09; el primer inicio válido que solapa es 10:25 (design D9).
        crear(cuerpo("10:00", 30)).andExpect(status().isCreated());
        rechazaSolapado(cuerpo("10:25", 30), "PROFESIONAL");
    }

    @Test
    @DisplayName("Un turno atendido sigue ocupando lugar")
    void unTurnoAtendidoSigueOcupandoLugar() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, lunes("10:00"), lunes("10:30"), "ATENDIDO");
        rechazaSolapado(cuerpo("10:00", 30), "PROFESIONAL");
    }

    // --- Tarea 7.1: acceso (RN-20, RN-33, regla dura 4) ---

    RequestPostProcessor sesionDe(String rol) {
        var usuario = DatosAgenda.usuario(jdbc, rol);
        return sesion((Long) usuario[0], (String) usuario[1], Rol.valueOf(rol));
    }

    @Test
    @DisplayName("El odontólogo no puede crear turnos")
    void elOdontologoNoPuedeCrearTurnos() throws Exception {
        crear(cuerpo("10:00", 30), sesionDe("ODONTOLOGO")).andExpect(status().isForbidden());
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("El servicio rechaza al odontólogo aunque se saltee el filtro HTTP")
    void elServicioRechazaAlOdontologo() {
        var odontologo = new UsernamePasswordAuthenticationToken("odontologo-ficticio", null,
                List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(odontologo);
        try {
            var solicitud = new ar.edu.utn.turnos.agenda.domain.SolicitudTurno(paciente, profesional, sillon, prestacion,
                    java.time.OffsetDateTime.parse(lunes("10:00")).toInstant(), 30);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> crearTurno.crear(solicitud, null, null))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("El administrador también puede crear turnos")
    void elAdministradorPuedeCrearTurnos() throws Exception {
        crear(cuerpo("10:00", 30), sesionDe("ADMIN")).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Sin sesión no se puede crear un turno")
    void sinSesionNoSePuedeCrearUnTurno() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo("10:00", 30)).with(csrfValido()))
                .andExpect(status().isUnauthorized());
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("Sin CSRF válido no se puede crear un turno")
    void sinCsrfValidoNoSePuedeCrearUnTurno() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo("10:00", 30))
                        .with(comoRecepcion).with(csrfConCabeceraIncorrecta()))
                .andExpect(status().isForbidden());
        assertThat(turnosDelProfesional()).isZero();
    }

    @Test
    @DisplayName("Un turno cancelado no ocupa lugar")
    void unTurnoCanceladoNoOcupaLugar() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, lunes("10:00"), lunes("10:30"), "CANCELADO");
        crear(cuerpo("10:00", 30)).andExpect(status().isCreated());
    }
}
