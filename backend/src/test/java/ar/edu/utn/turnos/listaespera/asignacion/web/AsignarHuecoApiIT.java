package ar.edu.utn.turnos.listaespera.asignacion.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@code POST /api/lista-espera/{id}/asignar} de punta a punta (C-43, RN-15, RN-22, RN-24). Sin
 * {@code @Transactional}: se prueba la atomicidad real de la asignación. El reloj fijo es el martes 1/12/2026
 * a las 09:00 de Buenos Aires; el hueco es el jueves 3/12 de 10:00 a 10:30.
 */
@Import(RelojFijoConfig.class)
class AsignarHuecoApiIT extends PruebaIntegracionBase {
    private static final String PERSONAL = "personal-ficticio";
    private static final String RUTA = "/api/lista-espera/{id}/asignar";
    private static final String INICIO = "2026-12-03T10:00:00-03:00";
    private static final Instant INICIO_INSTANT = Instant.parse("2026-12-03T13:00:00Z");
    private static final Instant FIN_INSTANT = INICIO_INSTANT.plus(Duration.ofMinutes(30));

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OfertasHuecoPersistencia ofertas;

    private Long paciente;
    private Long profesional;
    private Long sillon;
    private Long prestacion;
    private final List<Long> profesionales = new ArrayList<>();
    private final List<Long> sillones = new ArrayList<>();
    private final List<Long> prestaciones = new ArrayList<>();
    private final List<Long> entradas = new ArrayList<>();
    private final List<Long> pacientesExtra = new ArrayList<>();

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        profesionales.add(profesional);
        sillon = DatosAgenda.sillon(jdbc, true);
        sillones.add(sillon);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        prestaciones.add(prestacion);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        DatosAgenda.horario(jdbc, profesional, 4, "08:00", "18:00");
    }

    @AfterEach
    void limpiar() {
        for (Long p : profesionales) {
            jdbc.update("delete from oferta_hueco where profesional_id = ?", p);
        }
        for (Long e : entradas) {
            jdbc.update("delete from lista_espera where id = ?", e);
        }
        DatosAgenda.limpiar(jdbc, paciente, profesionales.toArray(Long[]::new), sillones.toArray(Long[]::new),
                prestaciones.toArray(Long[]::new));
        for (Long p : pacientesExtra) {
            jdbc.update("delete from pacientes where id = ?", p);
        }
    }

    // --- Datos ---

    private Long entrada(String estado, Long profPreferido) {
        var id = DatosFicticios.entrada(jdbc, paciente, prestacion, profPreferido, "2026-12-01", "2026-12-31",
                null, null, estado, "2026-11-01T10:00:00Z");
        entradas.add(id);
        return id;
    }

    private Long entradaDe(Long pacienteId, String estado, Long profPreferido) {
        var id = DatosFicticios.entrada(jdbc, pacienteId, prestacion, profPreferido, "2026-12-01", "2026-12-31",
                null, null, estado, "2026-11-01T10:00:00Z");
        entradas.add(id);
        return id;
    }

    private Long pacienteExtra() {
        var id = DatosFicticios.paciente(jdbc, "asignar@correo.test");
        pacientesExtra.add(id);
        return id;
    }

    private Long profesionalExtra() {
        var id = DatosAgenda.profesional(jdbc, true);
        profesionales.add(id);
        return id;
    }

    private Long sillonExtra() {
        var id = DatosAgenda.sillon(jdbc, true);
        sillones.add(id);
        return id;
    }

    private Long oferta(Long entradaId, Long prof, Instant inicio, Instant fin, Long sillonSugerido) {
        var oferta = OfertaHueco.nueva(entradaId, prof, inicio, fin, sillonSugerido, hashUnico(),
                RelojFijoConfig.INSTANTE_FIJO);
        return ofertas.guardar(oferta).id();
    }

    private static String hashUnico() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private static String cuerpo(Object profesionalId, String inicio, Object sillonId, String notas) {
        var partes = new ArrayList<String>();
        if (profesionalId != null) {
            partes.add("\"profesionalId\":" + profesionalId);
        }
        if (inicio != null) {
            partes.add("\"inicio\":\"" + inicio + "\"");
        }
        if (sillonId != null) {
            partes.add("\"sillonId\":" + sillonId);
        }
        if (notas != null) {
            partes.add("\"notas\":\"" + notas + "\"");
        }
        return "{" + String.join(",", partes) + "}";
    }

    // --- Peticiones y lecturas ---

    private ResultActions asignar(Long entradaId, String json, String rol) throws Exception {
        return mockMvc.perform(post(RUTA, entradaId)
                .with(csrfValido())
                .with(user(PERSONAL).roles(rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static long numero(MvcResult resultado, String ruta) throws Exception {
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), ruta)).longValue();
    }

    private String estadoEntrada(Long entrada) {
        return jdbc.queryForObject("select estado from lista_espera where id = ?", String.class, entrada);
    }

    private Long turnoAsignado(Long entrada) {
        return jdbc.queryForObject("select turno_asignado_id from lista_espera where id = ?", Long.class, entrada);
    }

    private String estadoOferta(Long entrada) {
        return jdbc.queryForObject("select estado from oferta_hueco where lista_espera_id = ?", String.class, entrada);
    }

    private long cantidadTurnos() {
        return jdbc.queryForObject("select count(*) from turno", Long.class);
    }

    private long cantidadTurnosProfesional() {
        return jdbc.queryForObject("select count(*) from turno where profesional_id = ?", Long.class, profesional);
    }

    private long cantidadTurnosHueco() {
        return jdbc.queryForObject("""
                select count(*) from turno
                where profesional_id = ? and inicio = cast(? as timestamptz)
                """, Long.class, profesional, INICIO_INSTANT.toString());
    }

    private long cantidadOfertas() {
        return jdbc.queryForObject("select count(*) from oferta_hueco where profesional_id = ?", Long.class,
                profesional);
    }

    private long cantidadOfertasTomadas() {
        return jdbc.queryForObject("""
                select count(*) from oferta_hueco
                where profesional_id = ? and hueco_inicio = cast(? as timestamptz) and estado = 'TOMADA'
                """, Long.class, profesional, INICIO_INSTANT.toString());
    }

    // --- 3.1 ---

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Recepción asigna un hueco a una entrada compatible")
    void asignarHueco(String rol) throws Exception {
        var entrada = entrada("ACTIVA", null);

        var resultado = asignar(entrada, cuerpo(profesional, INICIO, sillon, null), rol)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.pacienteId").value(paciente))
                .andExpect(jsonPath("$.profesionalId").value(profesional))
                .andExpect(jsonPath("$.prestacionId").value(prestacion))
                .andExpect(jsonPath("$.sillonId").value(sillon))
                .andExpect(jsonPath("$.inicio").value("2026-12-03T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2026-12-03T13:30:00Z"))
                .andExpect(jsonPath("$.duracionMin").value(30))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andReturn();

        var turnoId = numero(resultado, "$.id");
        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
        assertThat(turnoAsignado(entrada)).isEqualTo(turnoId);
    }

    @Test
    @DisplayName("La asignación guarda las notas del turno")
    void guardarNotas() throws Exception {
        var entrada = entrada("ACTIVA", null);

        var resultado = asignar(entrada, cuerpo(profesional, INICIO, sillon, "Llamar antes"), "RECEPCION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").value("Llamar antes"))
                .andReturn();

        var turnoId = numero(resultado, "$.id");
        assertThat(jdbc.queryForObject("select notas from turno where id = ?", String.class, turnoId))
                .isEqualTo("Llamar antes");
        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
    }

    // --- 3.2 ---

    @Test
    @DisplayName("El cuerpo exige profesional e inicio con zona")
    void cuerpoInvalido() throws Exception {
        var entrada = entrada("ACTIVA", null);

        for (var json : List.of(
                cuerpo(null, INICIO, sillon, null),
                cuerpo(profesional, null, sillon, null),
                cuerpo(profesional, "2026-12-03T10:00:00", sillon, null))) {
            asignar(entrada, json, "RECEPCION")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        assertThat(cantidadTurnos()).isZero();
        assertThat(estadoEntrada(entrada)).isEqualTo("ACTIVA");
    }

    // --- 3.3 ---

    @Test
    @DisplayName("Una entrada inexistente responde 404")
    void entradaInexistente() throws Exception {
        asignar(999999L, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("ENTRADA_NO_ENCONTRADA"));

        assertThat(cantidadTurnos()).isZero();
    }

    @Test
    @DisplayName("Una entrada que ya no está activa responde 409")
    void entradaNoActiva() throws Exception {
        var entrada = entrada("CANCELADA", null);

        asignar(entrada, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("ENTRADA_NO_ACTIVA"));

        assertThat(cantidadTurnos()).isZero();
        assertThat(estadoEntrada(entrada)).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Una entrada incompatible con el hueco responde 422")
    void entradaIncompatible() throws Exception {
        var otro = profesionalExtra();
        var entrada = entrada("ACTIVA", otro);

        asignar(entrada, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INCOMPATIBLE"));

        assertThat(cantidadTurnos()).isZero();
        assertThat(estadoEntrada(entrada)).isEqualTo("ACTIVA");
    }

    // --- 3.4 ---

    @Test
    @DisplayName("La duración del turno es la de la prestación de la entrada")
    void duracionDeLaPrestacion() throws Exception {
        var entrada = entrada("ACTIVA", null);

        asignar(entrada, cuerpo(profesional, INICIO, null, null), "RECEPCION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMin").value(30))
                .andExpect(jsonPath("$.fin").value("2026-12-03T13:30:00Z"));

        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
    }

    @Test
    @DisplayName("Sin sillón se prefiere el sugerido por la oferta si sigue libre")
    void prefiereSillonSugerido() throws Exception {
        var sillon2 = sillonExtra();
        var entrada = entrada("ACTIVA", null);
        oferta(entrada, profesional, INICIO_INSTANT, FIN_INSTANT, sillon2);

        asignar(entrada, cuerpo(profesional, INICIO, null, null), "RECEPCION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sillonId").value(sillon2));

        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
    }

    @Test
    @DisplayName("Sin sillón libre sugerido se usa el primero activo libre")
    void usaPrimeroLibre() throws Exception {
        var sillon2 = sillonExtra();
        var otro = profesionalExtra();
        var otroPaciente = pacienteExtra();
        var entrada = entrada("ACTIVA", null);
        oferta(entrada, profesional, INICIO_INSTANT, FIN_INSTANT, sillon2);
        DatosAgenda.turno(jdbc, otroPaciente, otro, sillon2, prestacion, INICIO_INSTANT.toString(),
                FIN_INSTANT.toString(), "PROGRAMADO");

        asignar(entrada, cuerpo(profesional, INICIO, null, null), "RECEPCION")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sillonId").value(sillon));

        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
    }

    @Test
    @DisplayName("Un sillón indicado ocupado responde 409")
    void sillonIndicadoOcupado() throws Exception {
        var otro = profesionalExtra();
        var otroPaciente = pacienteExtra();
        var entrada = entrada("ACTIVA", null);
        DatosAgenda.turno(jdbc, otroPaciente, otro, sillon, prestacion, INICIO_INSTANT.toString(),
                FIN_INSTANT.toString(), "PROGRAMADO");

        asignar(entrada, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-01"));

        assertThat(cantidadTurnosProfesional()).isZero();
        assertThat(estadoEntrada(entrada)).isEqualTo("ACTIVA");
    }

    // --- 3.5 ---

    @Test
    @DisplayName("Una segunda asignación del mismo hueco responde 409")
    void segundaAsignacion() throws Exception {
        var primera = entrada("ACTIVA", null);
        var segunda = entradaDe(pacienteExtra(), "ACTIVA", null);

        asignar(primera, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isCreated());
        asignar(segunda, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-01"));

        assertThat(cantidadTurnosHueco()).isEqualTo(1);
        assertThat(estadoEntrada(segunda)).isEqualTo("ACTIVA");
        assertThat(estadoEntrada(primera)).isEqualTo("ASIGNADA");
    }

    @Test
    @DisplayName("Las ofertas del hueco pasan a TOMADA")
    void ofertasTomadas() throws Exception {
        var primera = entrada("ACTIVA", null);
        var segunda = entradaDe(pacienteExtra(), "ACTIVA", null);
        oferta(primera, profesional, INICIO_INSTANT, FIN_INSTANT, null);
        oferta(segunda, profesional, INICIO_INSTANT, FIN_INSTANT, null);

        asignar(primera, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isCreated());

        assertThat(cantidadOfertasTomadas()).isEqualTo(2);
        assertThat(estadoEntrada(primera)).isEqualTo("ASIGNADA");
    }

    @Test
    @DisplayName("Sin ofertas la asignación igual funciona")
    void sinOfertas() throws Exception {
        var entrada = entrada("ACTIVA", null);

        asignar(entrada, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isCreated());

        assertThat(estadoEntrada(entrada)).isEqualTo("ASIGNADA");
        assertThat(cantidadOfertas()).isZero();
    }

    // --- 3.6 ---

    @Test
    @DisplayName("Un fallo al crear el turno no deja la entrada cerrada ni las ofertas tomadas")
    void falloAtomico() throws Exception {
        var otro = profesionalExtra();
        var otroPaciente = pacienteExtra();
        var entrada = entrada("ACTIVA", null);
        oferta(entrada, profesional, INICIO_INSTANT, FIN_INSTANT, null);
        DatosAgenda.turno(jdbc, otroPaciente, otro, sillon, prestacion, INICIO_INSTANT.toString(),
                FIN_INSTANT.toString(), "PROGRAMADO");

        asignar(entrada, cuerpo(profesional, INICIO, sillon, null), "RECEPCION")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-01"));

        assertThat(cantidadTurnosProfesional()).isZero();
        assertThat(estadoEntrada(entrada)).isEqualTo("ACTIVA");
        assertThat(turnoAsignado(entrada)).isNull();
        assertThat(estadoOferta(entrada)).isEqualTo("ENVIADA");
    }
}
