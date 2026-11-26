package ar.edu.utn.turnos.listaespera.oferta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.CancelarTurnoService;
import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Armado común de los IT de ofertas de hueco. Aviso automático al cancelar un turno (C-36, US-012, RN-06, RN-23, RN-38) de punta a punta con PostgreSQL real.
 * Sin {@code @Transactional}: el oyente corre después del commit. En el perfil {@code test} el aviso es
 * sincrónico, así que al volver la cancelación las ofertas y los emails ya están. El reloj fijo es el martes
 * 1/12/2026 a las 09:00 de Buenos Aires; el turno a cancelar es el viernes 4/12 a las 09:00.
 */
@Import(RelojFijoConfig.class)
abstract class EscenarioOfertasIT extends PruebaIntegracionBase {
    static final Instant AHORA = RelojFijoConfig.INSTANTE_FIJO;
    static final Instant INICIO_HUECO = AHORA.plus(Duration.ofHours(72));
    static final Pattern ENLACE = Pattern.compile("https://turnos\\.consultorio\\.test/oferta/([A-Za-z0-9_-]{43})");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InMemoryNotificacionAdapter correo;

    @Autowired
    CancelarTurnoService cancelaciones;

    @Autowired
    PlatformTransactionManager transacciones;

    Long pacienteTurno;
    Long profesional;
    Long sillon;
    Long consulta;
    Long conducto;
    Long recepcionista;
    RequestPostProcessor comoRecepcion;
    String topeOriginal;
    final List<Long> pacientes = new ArrayList<>();
    final List<Long> entradas = new ArrayList<>();
    final List<Long> otrasPrestaciones = new ArrayList<>();

    @BeforeEach
    void preparar() {
        correo.limpiar();
        pacienteTurno = DatosAgenda.paciente(jdbc, true);
        // El buzón de estos escenarios mide los avisos de lista de espera.
        jdbc.update("update pacientes set email = null where id = ?", pacienteTurno);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        consulta = DatosAgenda.prestacion(jdbc, 30, true);
        conducto = DatosAgenda.prestacion(jdbc, 90, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, consulta);
        DatosAgenda.asignarPrestacion(jdbc, profesional, conducto);
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        recepcionista = (Long) usuario[0];
        comoRecepcion = authentication(new UsernamePasswordAuthenticationToken(
                new IdentidadPersonal(recepcionista, (String) usuario[1], Rol.RECEPCION), null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPCION"))));
        topeOriginal = jdbc.queryForObject(
                "select valor from configuracion where clave = 'lista_espera.max_avisos_por_hueco'", String.class);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("update configuracion set valor = ? where clave = 'lista_espera.max_avisos_por_hueco'",
                topeOriginal);
        jdbc.update("""
                delete from notificacion_enviada
                where oferta_hueco_id in (select id from oferta_hueco where profesional_id = ?)
                """, profesional);
        jdbc.update("delete from oferta_hueco where profesional_id = ?", profesional);
        for (Long entrada : entradas) {
            jdbc.update("delete from lista_espera where id = ?", entrada);
        }
        for (Long paciente : pacientes) {
            jdbc.update("delete from pacientes where id = ?", paciente);
        }
        var prestaciones = new ArrayList<>(List.of(consulta, conducto));
        prestaciones.addAll(otrasPrestaciones);
        DatosAgenda.limpiar(jdbc, pacienteTurno, new Long[] {profesional}, new Long[] {sillon},
                prestaciones.toArray(Long[]::new));
        jdbc.update("delete from usuario where id = ?", recepcionista);
        correo.limpiar();
    }

    // --- Datos ---

    Long turno() {
        return DatosAgenda.turno(jdbc, pacienteTurno, profesional, sillon, consulta, INICIO_HUECO.toString(),
                INICIO_HUECO.plus(Duration.ofMinutes(30)).toString(), "PROGRAMADO");
    }

    Long paciente(String email) {
        var id = DatosFicticios.paciente(jdbc, email);
        pacientes.add(id);
        return id;
    }

    Long entrada(Long paciente, Long prestacion, String creadoEn) {
        var id = DatosFicticios.entrada(jdbc, paciente, prestacion, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", creadoEn);
        entradas.add(id);
        return id;
    }

    String email(Long paciente) {
        return jdbc.queryForObject("select email from pacientes where id = ?", String.class, paciente);
    }

    ResultActions cancelar(Long turno) throws Exception {
        return mockMvc.perform(post("/api/turnos/{id}/cancelar", turno).with(comoRecepcion).with(csrfValido()));
    }

    List<Map<String, Object>> ofertas() {
        return jdbc.queryForList("select * from oferta_hueco where profesional_id = ? order by id", profesional);
    }

    List<Long> entradasConOferta() {
        return jdbc.queryForList("select lista_espera_id from oferta_hueco where profesional_id = ? order by id",
                Long.class, profesional);
    }

    List<String> destinatarios() {
        return correo.enviados().stream().map(MensajeNotificacion::destinatario).toList();
    }

    static String token(MensajeNotificacion mensaje) {
        var m = ENLACE.matcher(mensaje.cuerpo());
        assertThat(m.find()).as("el email lleva el enlace de la oferta").isTrue();
        return m.group(1);
    }

}
