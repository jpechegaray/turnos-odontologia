package ar.edu.utn.turnos.reservapublica.token;

import static ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig.INSTANTE_FIJO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.TurnosParaToken;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(RelojFijoConfig.class)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ServicioTokenTurnoIT extends PruebaIntegracionBase {
    private static final Instant INICIO = INSTANTE_FIJO.plusSeconds(3600);
    private static final ServicioToken CRIPTO = new ServicioToken();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioTokenTurno servicio;

    @Autowired
    private TurnosParaToken turnos;

    @Autowired
    private TokenTurnoRepositorio tokens;

    @Test
    @DisplayName("La emisión entrega el secreto y guarda solo su hash")
    void emision() {
        Long turno = nuevoTurno(INICIO, "PROGRAMADO");
        var emitido = servicio.emitir(turno);

        assertThat(emitido.claro()).matches("[A-Za-z0-9_-]{43}");
        assertThat(emitido.toString()).doesNotContain(emitido.claro());
        var fila = jdbc.queryForMap("select * from token_turno where turno_id = ?", turno);
        assertThat(fila).containsEntry("token_hash", CRIPTO.hashear(emitido.claro()))
                .containsEntry("revocado_en", null).containsEntry("ultimo_uso_en", null)
                .doesNotContainValue(emitido.claro());
        assertThat(((Timestamp) fila.get("expira_en")).toInstant()).isEqualTo(INICIO);
        assertThat(((Timestamp) fila.get("creado_en")).toInstant()).isEqualTo(INSTANTE_FIJO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inexistente", "cancelado", "en el inicio", "pasado"})
    @DisplayName("No se emite para un turno inexistente, cancelado o iniciado")
    void noEmite(String caso) {
        Long turno = switch (caso) {
            case "inexistente" -> -1L;
            case "cancelado" -> nuevoTurno(INICIO, "CANCELADO");
            case "en el inicio" -> nuevoTurno(INSTANTE_FIJO, "PROGRAMADO");
            case "pasado" -> nuevoTurno(INSTANTE_FIJO.minusSeconds(60), "PROGRAMADO");
            default -> throw new AssertionError(caso);
        };
        assertThatThrownBy(() -> servicio.emitir(turno)).isInstanceOf(ReglaNegocioException.class);
        assertThat(jdbc.queryForObject("select count(*) from token_turno where turno_id = ?", Long.class, turno))
                .isZero();
    }

    @Test
    @DisplayName("Un token válido identifica solo su turno y registra el uso")
    void validacionYReutilizacion() {
        Long turno = nuevoTurno(INICIO, "PROGRAMADO");
        String secreto = servicio.emitir(turno).claro();
        assertThat(servicio.validar(secreto)).isEqualTo(turno);
        assertThat(ultimoUso(turno)).isEqualTo(INSTANTE_FIJO);

        Instant otroUso = INSTANTE_FIJO.plusSeconds(30);
        assertThat(conRelojEn(otroUso).validar(secreto, turno)).isEqualTo(turno);
        assertThat(ultimoUso(turno)).isEqualTo(otroUso);
    }

    @Test
    @DisplayName("El token vence exactamente al inicio del turno")
    void limiteDeVigencia() {
        Long turno = nuevoTurno(INICIO, "PROGRAMADO");
        String secreto = servicio.emitir(turno).claro();
        Instant antes = INICIO.minusNanos(1000);
        assertThat(conRelojEn(antes).validar(secreto)).isEqualTo(turno);
        assertThatThrownBy(() -> conRelojEn(INICIO).validar(secreto)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> conRelojEn(INICIO.plusSeconds(1)).validar(secreto))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(ultimoUso(turno)).isEqualTo(antes);
    }

    @Test
    @DisplayName("El token del turno A no abre el turno B")
    void turnoAjeno() {
        Long turnoA = nuevoTurno(INICIO, "PROGRAMADO");
        Long turnoB = nuevoTurno(INICIO, "PROGRAMADO");
        String secreto = servicio.emitir(turnoA).claro();
        var reloj = conRelojEn(INSTANTE_FIJO);
        var rechazo = assertThrows(ReglaNegocioException.class,
                () -> reloj.validar(secreto, turnoB));
        var inexistente = assertThrows(ReglaNegocioException.class,
                () -> reloj.validar(CRIPTO.generar().claro(), turnoB));
        assertThat(rechazo.estado()).isEqualTo(inexistente.estado());
        assertThat(rechazo.codigo()).isEqualTo(inexistente.codigo());
        assertThat(rechazo.getMessage()).isEqualTo(inexistente.getMessage());
        assertThat(ultimoUso(turnoA)).isNull();
    }

    @Test
    @DisplayName("Los rechazos de token son indistinguibles")
    void rechazosUniformes() {
        var enHorario = conRelojEn(INSTANTE_FIJO);
        Long turno = nuevoTurno(INICIO, "PROGRAMADO");
        Long otroTurno = nuevoTurno(INICIO, "PROGRAMADO");
        String vigente = servicio.emitir(turno).claro();
        String revocado = servicio.emitir(turno).claro();
        jdbc.update("update token_turno set revocado_en = ? where token_hash = ?",
                Timestamp.from(INSTANTE_FIJO), CRIPTO.hashear(revocado));
        var inexistente = assertThrows(ReglaNegocioException.class,
                () -> enHorario.validar(CRIPTO.generar().claro()));
        assertThat(inexistente.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
        for (var rechazo : java.util.List.of(
                assertThrows(ReglaNegocioException.class, () -> enHorario.validar(null)),
                assertThrows(ReglaNegocioException.class, () -> enHorario.validar(" ".repeat(43))),
                assertThrows(ReglaNegocioException.class, () -> enHorario.validar(vigente + "=")),
                assertThrows(ReglaNegocioException.class, () -> conRelojEn(INICIO).validar(vigente)),
                assertThrows(ReglaNegocioException.class, () -> enHorario.validar(revocado)),
                assertThrows(ReglaNegocioException.class, () -> enHorario.validar(vigente, otroTurno)))) {
            assertThat(rechazo.estado()).isEqualTo(inexistente.estado());
            assertThat(rechazo.codigo()).isEqualTo(inexistente.codigo());
            assertThat(rechazo.getMessage()).isEqualTo(inexistente.getMessage());
            assertThat(rechazo.propiedades()).isEmpty();
        }
    }

    @Test
    @DisplayName("El secreto no aparece en logs ni en errores del servicio")
    void noFiltraSecreto(CapturedOutput salida) {
        Long turno = nuevoTurno(INICIO, "PROGRAMADO");
        var emitido = servicio.emitir(turno);
        String rechazado = CRIPTO.generar().claro();
        assertThat(servicio.validar(emitido.claro())).isEqualTo(turno);
        var error = assertThrows(ReglaNegocioException.class, () -> servicio.validar(rechazado));
        assertThat(error.getMessage()).doesNotContain(emitido.claro(), rechazado);
        assertThat(emitido.toString()).doesNotContain(emitido.claro());
        assertThat(salida.getAll()).doesNotContain(emitido.claro(), rechazado);
    }

    private ServicioTokenTurno conRelojEn(Instant instante) {
        return new ServicioTokenTurno(turnos, tokens, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private Instant ultimoUso(Long turno) {
        Timestamp marca = jdbc.queryForObject("select ultimo_uso_en from token_turno where turno_id = ?",
                Timestamp.class, turno);
        return marca == null ? null : marca.toInstant();
    }

    private Long nuevoTurno(Instant inicio, String estado) {
        return DatosAgenda.turno(jdbc, DatosAgenda.paciente(jdbc, true), DatosAgenda.profesional(jdbc, true),
                DatosAgenda.sillon(jdbc, true), DatosAgenda.prestacion(jdbc, 30, true),
                inicio.toString(), inicio.plusSeconds(1800).toString(), estado);
    }
}
