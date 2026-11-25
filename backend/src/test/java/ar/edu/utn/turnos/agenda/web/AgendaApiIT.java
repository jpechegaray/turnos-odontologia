package ar.edu.utn.turnos.agenda.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda diaria y semanal (C-28, US-013). Datos ficticios en diciembre de 2026: el 7 es lunes y el reloj
 * fijo de {@link RelojFijoConfig} hace que hoy sea martes 2026-12-01 en Buenos Aires. Las consultas filtran
 * por los profesionales creados acá, salvo las que prueban la vista sin filtros.
 */
@Transactional
@Import(RelojFijoConfig.class)
class AgendaApiIT extends PruebaIntegracionBase {
    private static final String RUTA = "/api/agenda";
    private static final String PERSONAL = "personal-ficticio";

    @Autowired
    private JdbcTemplate jdbc;

    private Long paciente;
    private Long otroPaciente;
    private Long profesional;
    private Long otroProfesional;
    private Long sillon;
    private Long otroSillon;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        otroPaciente = jdbc.queryForObject("""
                insert into pacientes (nombre, apellido, email, telefono, activo, creado_en)
                values ('Bruno', 'Inventado', 'bruno.agenda@correo.test', '1100000001', true, now()) returning id
                """, Long.class);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        otroSillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    // --- Datos y pedidos ---

    private Long turno(Long pac, Long prof, Long sil, String inicio, String fin, String estado) {
        Long id = DatosAgenda.turno(jdbc, pac, prof, sil, prestacion, inicio, fin, estado);
        jdbc.update("update turno set notas = ? where id = ?", "Notas ficticias " + id, id);
        return id;
    }

    private Long turno(Long prof, Long sil, String inicio, String fin) {
        return turno(paciente, prof, sil, inicio, fin, "PROGRAMADO");
    }

    private void bloqueo(Long prof, String inicio, String fin, String motivo) {
        jdbc.update("""
                insert into bloqueo (profesional_id, inicio, fin, motivo)
                values (?, cast(? as timestamptz), cast(? as timestamptz), ?)
                """, prof, inicio, fin, motivo);
    }

    /** Odontólogo ficticio con sesión real (C-03), vinculado al profesional indicado o sin vínculo. */
    private RequestPostProcessor comoOdontologo(Long profesionalVinculado) {
        String email = "odontologo-" + System.nanoTime() + "@consultorio.test";
        Long id = jdbc.queryForObject("""
                insert into usuario (email, password_hash, nombre, rol, profesional_id, activo, intentos_fallidos,
                                     creado_en)
                values (?, ?, 'Odontólogo Ficticio', 'ODONTOLOGO', ?, true, 0, now()) returning id
                """, Long.class, email, "$2a$10$" + "x".repeat(53), profesionalVinculado);
        return TurnosApiIT.sesion(id, email, Rol.ODONTOLOGO);
    }

    private static MockHttpServletRequestBuilder agenda(String rol) {
        return get(RUTA).with(user(PERSONAL).roles(rol));
    }

    private static MockHttpServletRequestBuilder dia(String rol, String fecha) {
        return agenda(rol).param("vista", "dia").param("fecha", fecha);
    }

    private static int entero(Long id) {
        return id.intValue();
    }

    // --- Vistas ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("La vista diaria devuelve turnos, horarios y bloqueos del día")
    void vistaDiaria(String rol) throws Exception {
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "13:00");
        Long turno = turno(profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");
        bloqueo(profesional, "2026-12-07T12:00:00-03:00", "2026-12-07T13:00:00-03:00", "Capacitación");

        mockMvc.perform(dia(rol, "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("dia"))
                .andExpect(jsonPath("$.desde").value("2026-12-07"))
                .andExpect(jsonPath("$.hasta").value("2026-12-07"))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(turno))))
                .andExpect(jsonPath("$.turnos[0].estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-12-07T10:00:00-03:00"))
                .andExpect(jsonPath("$.turnos[0].fin").value("2026-12-07T10:30:00-03:00"))
                .andExpect(jsonPath("$.turnos[0].duracionMin").value(30))
                .andExpect(jsonPath("$.turnos[0].profesionalId").value(profesional))
                .andExpect(jsonPath("$.turnos[0].sillonId").value(sillon))
                .andExpect(jsonPath("$.turnos[0].prestacionId").value(prestacion))
                .andExpect(jsonPath("$.turnos[0].pacienteId").value(paciente))
                .andExpect(jsonPath("$.horarios", hasSize(1)))
                .andExpect(jsonPath("$.horarios[0].profesionalId").value(profesional))
                .andExpect(jsonPath("$.horarios[0].fecha").value("2026-12-07"))
                .andExpect(jsonPath("$.horarios[0].inicio").value("2026-12-07T09:00:00-03:00"))
                .andExpect(jsonPath("$.horarios[0].fin").value("2026-12-07T13:00:00-03:00"))
                .andExpect(jsonPath("$.bloqueos", hasSize(1)))
                .andExpect(jsonPath("$.bloqueos[0].id").isNumber())
                .andExpect(jsonPath("$.bloqueos[0].profesionalId").value(profesional))
                .andExpect(jsonPath("$.bloqueos[0].inicio").value("2026-12-07T12:00:00-03:00"))
                .andExpect(jsonPath("$.bloqueos[0].fin").value("2026-12-07T13:00:00-03:00"));
    }

    @Test
    @DisplayName("La vista semanal va de lunes a domingo")
    void vistaSemanal() throws Exception {
        Long lunes = turno(profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");
        Long domingo = turno(profesional, sillon, "2026-12-13T10:00:00-03:00", "2026-12-13T10:30:00-03:00");
        turno(profesional, sillon, "2026-12-14T10:00:00-03:00", "2026-12-14T10:30:00-03:00");

        mockMvc.perform(agenda("RECEPCION").param("vista", "semana").param("fecha", "2026-12-10")
                        .param("profesionalId", profesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("semana"))
                .andExpect(jsonPath("$.desde").value("2026-12-07"))
                .andExpect(jsonPath("$.hasta").value("2026-12-13"))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(lunes), entero(domingo))));
    }

    @Test
    @DisplayName("Sin parámetros se consulta el día de hoy")
    void sinParametrosEsHoy() throws Exception {
        Long hoy = turno(profesional, sillon, "2026-12-01T15:00:00-03:00", "2026-12-01T15:30:00-03:00");
        Long manana = turno(profesional, sillon, "2026-12-02T10:00:00-03:00", "2026-12-02T10:30:00-03:00");

        mockMvc.perform(agenda("ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("dia"))
                .andExpect(jsonPath("$.desde").value("2026-12-01"))
                .andExpect(jsonPath("$.hasta").value("2026-12-01"))
                .andExpect(jsonPath("$.turnos[*].id").value(hasItem(entero(hoy))))
                .andExpect(jsonPath("$.turnos[*].id").value(not(hasItem(entero(manana)))));
    }

    @ParameterizedTest
    @CsvSource({"vista,mes", "fecha,07/12/2026", "fecha,2026-13-01"})
    @DisplayName("Parámetros inválidos responden 400")
    void parametrosInvalidos(String parametro, String valor) throws Exception {
        mockMvc.perform(agenda("RECEPCION").param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // --- Hora local ---

    @Test
    @DisplayName("Un turno a las 23:30 hora local pertenece a ese día")
    void turnoDeLas2330() throws Exception {
        Long tarde = turno(profesional, sillon, "2026-12-07T23:30:00-03:00", "2026-12-07T23:55:00-03:00");

        mockMvc.perform(dia("RECEPCION", "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(tarde))))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2026-12-07T23:30:00-03:00"));
        mockMvc.perform(dia("RECEPCION", "2026-12-08").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos", hasSize(0)));
    }

    @Test
    @DisplayName("Un turno a las 00:00 hora local pertenece al día que empieza")
    void turnoDeMedianoche() throws Exception {
        Long medianoche = turno(profesional, sillon, "2026-12-08T00:00:00-03:00", "2026-12-08T00:30:00-03:00");

        mockMvc.perform(dia("RECEPCION", "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos", hasSize(0)));
        mockMvc.perform(dia("RECEPCION", "2026-12-08").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(medianoche))));
    }

    @Test
    @DisplayName("Un bloqueo de varios días aparece en cada día que toca")
    void bloqueoDeVariosDias() throws Exception {
        bloqueo(profesional, "2026-12-07T15:00:00-03:00", "2026-12-09T10:00:00-03:00", "Congreso");

        mockMvc.perform(dia("RECEPCION", "2026-12-08").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.bloqueos", hasSize(1)))
                .andExpect(jsonPath("$.bloqueos[0].inicio").value("2026-12-07T15:00:00-03:00"))
                .andExpect(jsonPath("$.bloqueos[0].fin").value("2026-12-09T10:00:00-03:00"));
    }

    // --- Contenido ---

    @Test
    @DisplayName("Los turnos de cualquier estado se muestran con su estado")
    void turnosDeCualquierEstado() throws Exception {
        Long programado = turno(paciente, profesional, sillon, "2026-12-07T09:00:00-03:00",
                "2026-12-07T09:30:00-03:00", "PROGRAMADO");
        Long cancelado = turno(paciente, profesional, sillon, "2026-12-07T08:00:00-03:00",
                "2026-12-07T08:30:00-03:00", "CANCELADO");

        mockMvc.perform(dia("RECEPCION", "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(cancelado), entero(programado))))
                .andExpect(jsonPath("$.turnos[*].estado").value(contains("CANCELADO", "PROGRAMADO")));
    }

    @Test
    @DisplayName("Un horario semanal se proyecta en cada fecha del período")
    void horarioSemanalProyectado() throws Exception {
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "13:00");
        DatosAgenda.horario(jdbc, profesional, 3, "09:00", "13:00");

        mockMvc.perform(agenda("RECEPCION").param("vista", "semana").param("fecha", "2026-12-07")
                        .param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.horarios[*].fecha").value(contains("2026-12-07", "2026-12-09")))
                .andExpect(jsonPath("$.horarios[0].inicio").value("2026-12-07T09:00:00-03:00"))
                .andExpect(jsonPath("$.horarios[0].fin").value("2026-12-07T13:00:00-03:00"));
    }

    // --- Filtros ---

    @Test
    @DisplayName("Filtrar por profesional muestra solo su agenda")
    void filtroPorProfesional() throws Exception {
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "13:00");
        DatosAgenda.horario(jdbc, otroProfesional, 1, "14:00", "18:00");
        Long propio = turno(profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");
        turno(otroProfesional, otroSillon, "2026-12-07T15:00:00-03:00", "2026-12-07T15:30:00-03:00");
        bloqueo(profesional, "2026-12-07T12:00:00-03:00", "2026-12-07T13:00:00-03:00", "Propio");
        bloqueo(otroProfesional, "2026-12-07T17:00:00-03:00", "2026-12-07T18:00:00-03:00", "Ajeno");

        mockMvc.perform(dia("RECEPCION", "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(propio))))
                .andExpect(jsonPath("$.horarios[*].profesionalId").value(contains(entero(profesional))))
                .andExpect(jsonPath("$.bloqueos[*].motivo").value(contains("Propio")));
    }

    @Test
    @DisplayName("Filtrar por sillón muestra solo los turnos de ese sillón")
    void filtroPorSillon() throws Exception {
        Long enSillon = turno(profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");
        turno(otroProfesional, otroSillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");

        mockMvc.perform(dia("RECEPCION", "2026-12-07").param("sillonId", sillon.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(enSillon))));
    }

    @Test
    @DisplayName("Los filtros de profesional y sillón se combinan")
    void filtrosCombinados() throws Exception {
        Long buscado = turno(profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00");
        turno(profesional, otroSillon, "2026-12-07T11:00:00-03:00", "2026-12-07T11:30:00-03:00");
        turno(otroProfesional, sillon, "2026-12-07T12:00:00-03:00", "2026-12-07T12:30:00-03:00");

        mockMvc.perform(dia("ADMIN", "2026-12-07").param("profesionalId", profesional.toString())
                        .param("sillonId", sillon.toString()))
                .andExpect(jsonPath("$.turnos[*].id").value(contains(entero(buscado))));
    }

    @Test
    @DisplayName("Un profesional sin datos devuelve listas vacías")
    void profesionalSinDatos() throws Exception {
        mockMvc.perform(agenda("RECEPCION").param("vista", "semana").param("fecha", "2026-12-07")
                        .param("profesionalId", profesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos", hasSize(0)))
                .andExpect(jsonPath("$.horarios", hasSize(0)))
                .andExpect(jsonPath("$.bloqueos", hasSize(0)));
    }

    // --- Visibilidad por rol ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Recepción ve el paciente de todos los turnos")
    void recepcionVeTodo(String rol) throws Exception {
        Long propio = turno(paciente, profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00",
                "PROGRAMADO");
        Long ajeno = turno(otroPaciente, otroProfesional, otroSillon, "2026-12-07T11:00:00-03:00",
                "2026-12-07T11:30:00-03:00", "PROGRAMADO");
        bloqueo(otroProfesional, "2026-12-07T15:00:00-03:00", "2026-12-07T16:00:00-03:00", "Trámite personal");

        mockMvc.perform(dia(rol, "2026-12-07").param("profesionalId", profesional.toString()))
                .andExpect(jsonPath("$.turnos[0].paciente.nombre").value("Ana"))
                .andExpect(jsonPath("$.turnos[0].paciente.apellido").value("Ficticia"))
                .andExpect(jsonPath("$.turnos[0].notas").value("Notas ficticias " + propio));
        mockMvc.perform(dia(rol, "2026-12-07").param("profesionalId", otroProfesional.toString()))
                .andExpect(jsonPath("$.turnos[0].paciente.nombre").value("Bruno"))
                .andExpect(jsonPath("$.turnos[0].paciente.apellido").value("Inventado"))
                .andExpect(jsonPath("$.turnos[0].notas").value("Notas ficticias " + ajeno))
                .andExpect(jsonPath("$.bloqueos[0].motivo").value("Trámite personal"));
    }

    @Test
    @DisplayName("El odontólogo ve el paciente solo en sus turnos")
    void odontologoVeSoloLoPropio() throws Exception {
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "13:00");
        DatosAgenda.horario(jdbc, otroProfesional, 1, "14:00", "18:00");
        Long propio = turno(paciente, profesional, sillon, "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00",
                "PROGRAMADO");
        Long ajeno = turno(otroPaciente, otroProfesional, otroSillon, "2026-12-07T15:00:00-03:00",
                "2026-12-07T15:30:00-03:00", "PROGRAMADO");
        bloqueo(profesional, "2026-12-07T12:00:00-03:00", "2026-12-07T13:00:00-03:00", "Propio");
        bloqueo(otroProfesional, "2026-12-07T17:00:00-03:00", "2026-12-07T18:00:00-03:00", "Vacaciones");
        var odontologo = comoOdontologo(profesional);

        mockMvc.perform(get(RUTA).with(odontologo).param("fecha", "2026-12-07")
                        .param("profesionalId", profesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[0].id").value(propio))
                .andExpect(jsonPath("$.turnos[0].paciente.nombre").value("Ana"))
                .andExpect(jsonPath("$.turnos[0].notas").value("Notas ficticias " + propio))
                .andExpect(jsonPath("$.bloqueos[0].motivo").value("Propio"));
        mockMvc.perform(get(RUTA).with(odontologo).param("fecha", "2026-12-07")
                        .param("profesionalId", otroProfesional.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos[0].id").value(ajeno))
                .andExpect(jsonPath("$.turnos[0].pacienteId").value(otroPaciente))
                .andExpect(jsonPath("$.turnos[0].paciente").value((Object) null))
                .andExpect(jsonPath("$.turnos[0].notas").value((Object) null))
                .andExpect(jsonPath("$.bloqueos[0].motivo").value((Object) null))
                .andExpect(jsonPath("$.horarios[*].profesionalId").value(contains(entero(otroProfesional))));
        mockMvc.perform(get(RUTA).with(odontologo).param("fecha", "2026-12-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios[*].profesionalId").value(hasItem(entero(profesional))))
                .andExpect(jsonPath("$.horarios[*].profesionalId").value(hasItem(entero(otroProfesional))))
                .andExpect(jsonPath("$.turnos[*].id").value(hasItem(entero(propio))))
                .andExpect(jsonPath("$.turnos[*].id").value(hasItem(entero(ajeno))));
    }

    @Test
    @DisplayName("Un odontólogo sin profesional vinculado recibe 403")
    void odontologoSinVinculo() throws Exception {
        mockMvc.perform(get(RUTA).with(user(PERSONAL).roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.turnos").doesNotExist());
    }

    @Test
    @DisplayName("Sin sesión la agenda responde 401")
    void sinSesion() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.turnos").doesNotExist());
    }
}
