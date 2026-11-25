package ar.edu.utn.turnos.agenda.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Alta interna de un turno online con sillón asignado por el sistema (C-42, spec agenda-turnos). Reloj fijo: martes
 * 1/12/2026 09:00 de Buenos Aires; los turnos son el lunes 7/12 a las 10:00 (13:00 UTC). Los sillones activos que
 * hubiera de otras pruebas se desactivan durante la prueba para que "el primero activo libre" sea determinista.
 */
@Import(RelojFijoConfig.class)
class CrearTurnoOnlineIT extends PruebaIntegracionBase {
    private static final Instant LUNES_10 = Instant.parse("2026-12-07T13:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CrearTurnoOnlineService altaOnline;

    Long paciente;
    Long otroPaciente;
    Long profesional;
    Long otroProfesional;
    Long prestacion;
    final List<Long> sillones = new ArrayList<>();
    final List<Long> ocupantes = new ArrayList<>();
    List<Long> sillonesAjenos;

    @BeforeEach
    void preparar() {
        sillonesAjenos = jdbc.queryForList("select id from sillon where activo", Long.class);
        jdbc.update("update sillon set activo = false where activo");
        paciente = DatosAgenda.paciente(jdbc, true);
        otroPaciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        for (Long p : List.of(profesional, otroProfesional)) {
            DatosAgenda.asignarPrestacion(jdbc, p, prestacion);
            DatosAgenda.horario(jdbc, p, 1, "09:00", "13:00");
        }
        for (int i = 0; i < 3; i++) {
            sillones.add(DatosAgenda.sillon(jdbc, true));
        }
    }

    @AfterEach
    void limpiar() {
        var profesionales = new ArrayList<>(List.of(profesional, otroProfesional));
        profesionales.addAll(ocupantes);
        DatosAgenda.limpiar(jdbc, paciente, profesionales.toArray(Long[]::new), sillones.toArray(Long[]::new),
                new Long[] {prestacion});
        jdbc.update("delete from pacientes where id = ?", otroPaciente);
        for (Long id : sillonesAjenos) {
            jdbc.update("update sillon set activo = true where id = ?", id);
        }
    }

    /** Ocupa el sillón con un turno de otro profesional (uno distinto por sillón, para no chocar por profesional). */
    private void ocupar(Long sillon) {
        var ocupante = DatosAgenda.profesional(jdbc, true);
        ocupantes.add(ocupante);
        DatosAgenda.turno(jdbc, otroPaciente, ocupante, sillon, prestacion, "2026-12-07T13:00:00Z",
                "2026-12-07T13:30:00Z", "PROGRAMADO");
    }

    @Test
    @DisplayName("Alta online en el sillón preferido")
    void sillonPreferido() {
        var turno = altaOnline.crear(paciente, profesional, prestacion, LUNES_10, sillones.get(1));

        assertThat(turno.id()).isNotNull();
        assertThat(turno.sillonId()).isEqualTo(sillones.get(1));
        assertThat(turno.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
        assertThat(turno.canal()).isEqualTo(CanalTurno.ONLINE);
        assertThat(turno.creadoPorUsuarioId()).isNull();
        assertThat(turno.intervalo().inicio()).isEqualTo(LUNES_10);
        assertThat(turno.intervalo().fin()).isEqualTo(Instant.parse("2026-12-07T13:30:00Z"));
        var fila = jdbc.queryForMap("select canal, estado, sillon_id, creado_por_usuario_id from turno where id = ?",
                turno.id());
        assertThat(fila.get("canal")).isEqualTo("ONLINE");
        assertThat(fila.get("estado")).isEqualTo("PROGRAMADO");
        assertThat(((Number) fila.get("sillon_id")).longValue()).isEqualTo(sillones.get(1));
        assertThat(fila.get("creado_por_usuario_id")).isNull();
    }

    @Test
    @DisplayName("Alta online sin sillón preferido libre")
    void primerSillonLibre() {
        ocupar(sillones.get(0));
        ocupar(sillones.get(1));

        var turno = altaOnline.crear(paciente, profesional, prestacion, LUNES_10, sillones.get(1));

        assertThat(turno.sillonId()).isEqualTo(sillones.get(2));
    }

    @Test
    @DisplayName("Alta online sin ningún sillón libre")
    void sinSillonLibre() {
        sillones.forEach(this::ocupar);

        assertThatThrownBy(() -> altaOnline.crear(paciente, profesional, prestacion, LUNES_10, null))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("RN-01");
                    assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(error.propiedades()).containsEntry("recurso", "SILLON");
                });
        assertThat(jdbc.queryForObject("select count(*) from turno where paciente_id = ?", Integer.class, paciente))
                .isZero();
    }

    @Test
    @DisplayName("Alta online solapada con otro turno del profesional")
    void solapadaConProfesional() {
        DatosAgenda.turno(jdbc, otroPaciente, profesional, sillones.get(0), prestacion, "2026-12-07T13:00:00Z",
                "2026-12-07T13:30:00Z", "PROGRAMADO");

        assertThatThrownBy(() -> altaOnline.crear(paciente, profesional, prestacion,
                Instant.parse("2026-12-07T13:15:00Z"), sillones.get(1)))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("RN-01");
                    assertThat(error.propiedades()).containsEntry("recurso", "PROFESIONAL");
                });
    }
}
