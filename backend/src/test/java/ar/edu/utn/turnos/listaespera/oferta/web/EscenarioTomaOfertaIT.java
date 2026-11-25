package ar.edu.utn.turnos.listaespera.oferta.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Armado común de los IT de la toma de una oferta de hueco (C-42, US-022, Flujo 7) con PostgreSQL real. Reloj fijo:
 * martes 1/12/2026 09:00 de Buenos Aires. El hueco ofrecido es el viernes 4/12 de 09:00 a 10:00 y la prestación dura
 * 30 minutos, así que el turno tomado va de 09:00 a 09:30. Las ofertas se insertan directo con un token conocido. Los
 * sillones activos de otras pruebas se desactivan durante cada prueba para que la elección del sillón sea
 * determinista. Cada pedido público sale de una IP distinta para no chocar con el límite de tasa (C-17).
 */
@Import(RelojFijoConfig.class)
abstract class EscenarioTomaOfertaIT extends PruebaIntegracionBase {
    static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;
    static final Instant INICIO_HUECO = Instant.parse("2026-12-04T12:00:00Z");
    static final Instant FIN_HUECO = INICIO_HUECO.plus(Duration.ofMinutes(60));
    static final Instant FIN_TURNO = INICIO_HUECO.plus(Duration.ofMinutes(30));
    static final String RUTA = "/api/public/ofertas/{token}";
    private static final AtomicInteger IPS = new AtomicInteger();

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InMemoryNotificacionAdapter correo;

    final ServicioToken tokens = new ServicioToken();

    Long profesional;
    Long sillon;
    Long consulta;
    Long pacienteAjeno;
    final List<Long> profesionales = new ArrayList<>();
    final List<Long> sillones = new ArrayList<>();
    final List<Long> pacientes = new ArrayList<>();
    final List<Long> entradas = new ArrayList<>();
    List<Long> sillonesAjenos;

    @BeforeEach
    void prepararEscenario() {
        correo.limpiar();
        sillonesAjenos = jdbc.queryForList("select id from sillon where activo", Long.class);
        jdbc.update("update sillon set activo = false where activo");
        profesional = nuevoProfesional();
        sillon = nuevoSillon();
        consulta = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, consulta);
        pacienteAjeno = DatosAgenda.paciente(jdbc, true);
    }

    @AfterEach
    void limpiarEscenario() {
        var turnos = new java.util.LinkedHashSet<Long>();
        profesionales.forEach(p -> turnos.addAll(
                jdbc.queryForList("select id from turno where profesional_id = ?", Long.class, p)));
        pacientes.forEach(p -> turnos.addAll(
                jdbc.queryForList("select id from turno where paciente_id = ?", Long.class, p)));
        for (Long turno : turnos) {
            jdbc.update("delete from notificacion_enviada where turno_id = ?", turno);
            jdbc.update("delete from token_turno where turno_id = ?", turno);
        }
        for (Long p : profesionales) {
            jdbc.update("""
                    delete from notificacion_enviada
                    where oferta_hueco_id in (select id from oferta_hueco where profesional_id = ?)
                    """, p);
            jdbc.update("delete from oferta_hueco where profesional_id = ?", p);
        }
        for (Long entrada : entradas) {
            jdbc.update("delete from lista_espera where id = ?", entrada);
        }
        for (Long turno : turnos) {
            jdbc.update("delete from turno where id = ?", turno);
        }
        for (Long paciente : pacientes) {
            jdbc.update("delete from notificacion_enviada where paciente_id = ?", paciente);
            jdbc.update("delete from pacientes where id = ?", paciente);
        }
        DatosAgenda.limpiar(jdbc, pacienteAjeno, profesionales.toArray(Long[]::new), sillones.toArray(Long[]::new),
                new Long[] {consulta});
        for (Long id : sillonesAjenos) {
            jdbc.update("update sillon set activo = true where id = ?", id);
        }
        correo.limpiar();
    }

    // --- Datos ---

    /** Profesional activo que atiende martes y viernes de 08:00 a 13:00. */
    Long nuevoProfesional() {
        var id = DatosAgenda.profesional(jdbc, true);
        profesionales.add(id);
        DatosAgenda.horario(jdbc, id, 2, "08:00", "13:00");
        DatosAgenda.horario(jdbc, id, 5, "08:00", "13:00");
        return id;
    }

    Long nuevoSillon() {
        var id = DatosAgenda.sillon(jdbc, true);
        sillones.add(id);
        return id;
    }

    Long paciente(String email) {
        var id = DatosFicticios.paciente(jdbc, email);
        pacientes.add(id);
        return id;
    }

    Long entrada(Long paciente) {
        var id = DatosFicticios.entrada(jdbc, paciente, consulta, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", "2026-11-20T12:00:00Z");
        entradas.add(id);
        return id;
    }

    /** Oferta ENVIADA del hueco del viernes para una entrada nueva de un paciente nuevo; devuelve el token. */
    String ofertaDisponible() {
        return oferta(entrada(paciente("toma@lista.test")), INICIO_HUECO, FIN_HUECO, sillon);
    }

    /** Inserta una oferta ENVIADA que vence al inicio del hueco y devuelve el token en claro. */
    String oferta(Long entrada, Instant inicio, Instant fin, Long sillonSugerido) {
        var generado = tokens.generar();
        jdbc.update("""
                insert into oferta_hueco (lista_espera_id, profesional_id, hueco_inicio, hueco_fin, sillon_sugerido_id,
                                          token_hash, expira_en, estado, creado_en)
                values (?, ?, ?, ?, ?, ?, ?, 'ENVIADA', ?)
                """, entrada, profesional, Timestamp.from(inicio), Timestamp.from(fin), sillonSugerido,
                generado.hash(), Timestamp.from(inicio), Timestamp.from(AHORA));
        return generado.claro();
    }

    /** Turno de otro paciente insertado directo, sin validación. */
    Long turnoAjeno(Long profesionalId, Long sillonId, Instant inicio, Instant fin) {
        return DatosAgenda.turno(jdbc, pacienteAjeno, profesionalId, sillonId, consulta, inicio.toString(),
                fin.toString(), "PROGRAMADO");
    }

    // --- Consultas ---

    String estadoOferta(String token) {
        return jdbc.queryForObject("select estado from oferta_hueco where token_hash = ?", String.class,
                tokens.hashear(token));
    }

    Long entradaDe(String token) {
        return jdbc.queryForObject("select lista_espera_id from oferta_hueco where token_hash = ?", Long.class,
                tokens.hashear(token));
    }

    String estadoEntrada(Long entrada) {
        return jdbc.queryForObject("select estado from lista_espera where id = ?", String.class, entrada);
    }

    int turnosDelProfesional() {
        return jdbc.queryForObject("select count(*) from turno where profesional_id = ? and estado <> 'CANCELADO'",
                Integer.class, profesional);
    }

    // --- API ---

    ResultActions consultar(String token) throws Exception {
        return mockMvc.perform(get(RUTA, token).with(EscenarioTomaOfertaIT::ipNueva));
    }

    ResultActions tomar(String token) throws Exception {
        return mockMvc.perform(post(RUTA + "/tomar", token).with(EscenarioTomaOfertaIT::ipNueva));
    }

    static MockHttpServletRequest ipNueva(MockHttpServletRequest solicitud) {
        int n = IPS.incrementAndGet();
        solicitud.setRemoteAddr("10.42." + (n / 250) + "." + (n % 250 + 1));
        return solicitud;
    }
}
