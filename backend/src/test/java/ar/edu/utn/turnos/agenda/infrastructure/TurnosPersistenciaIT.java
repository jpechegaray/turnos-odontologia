package ar.edu.utn.turnos.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Persistencia de turnos con PostgreSQL real: guardado, consulta de ocupados y traducción de RN-15 a 409. */
class TurnosPersistenciaIT extends PruebaIntegracionBase {
    private static final Instant DIEZ = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant CREADO = Instant.parse("2030-06-01T12:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TurnosPersistencia persistencia;

    private Long paciente;
    private Long profesional;
    private Long otroProfesional;
    private Long sillon;
    private Long otroSillon;
    private Long tercerSillon;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        otroSillon = DatosAgenda.sillon(jdbc, true);
        tercerSillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional, otroProfesional},
                new Long[] {sillon, otroSillon, tercerSillon}, new Long[] {prestacion});
    }

    /** Turno nuevo de 10:00 a 10:30 (hora de Buenos Aires) del profesional y sillón indicados. */
    private Turno nuevo(Long prof, Long sil) {
        return Turno.nuevo(paciente, prof, sil, prestacion, Intervalo.desde(DIEZ, 30), "Notas ficticias", null, CREADO);
    }

    private Long existente(Long prof, Long sil, String inicio, String fin, String estado) {
        return DatosAgenda.turno(jdbc, paciente, prof, sil, prestacion, inicio, fin, estado);
    }

    private static void esSolapado(Throwable error, String recurso) {
        assertThat(error).isInstanceOf(ReglaNegocioException.class);
        var regla = (ReglaNegocioException) error;
        assertThat(regla.estado()).isEqualTo(EstadoError.CONFLICTO_409);
        assertThat(regla.codigo()).isEqualTo("RN-01");
        assertThat(regla.propiedades()).containsEntry("recurso", recurso);
    }

    @Test
    @DisplayName("Guardar un turno le asigna id y versión, y se lee igual que se guardó")
    void guardaYLee() {
        var guardado = persistencia.guardar(nuevo(profesional, sillon));

        assertThat(guardado.id()).isNotNull();
        assertThat(guardado.version()).isNotNull();
        assertThat(guardado.estado()).isEqualTo(EstadoTurno.PROGRAMADO);
        assertThat(guardado.intervalo()).isEqualTo(Intervalo.desde(DIEZ, 30));
        assertThat(persistencia.buscar(guardado.id())).contains(guardado);
        var fila = jdbc.queryForMap("select estado, canal, duracion_min, notas from turno where id = ?", guardado.id());
        assertThat(fila).containsEntry("estado", "PROGRAMADO").containsEntry("canal", "INTERNO")
                .containsEntry("duracion_min", 30).containsEntry("notas", "Notas ficticias");
    }

    @Test
    @DisplayName("La base rechaza un solapamiento que esquivó la validación")
    void baseRechazaPorSillon() {
        existente(otroProfesional, sillon, "2030-06-10T13:15:00Z", "2030-06-10T13:45:00Z", "PROGRAMADO");
        assertThatThrownBy(() -> persistencia.guardar(nuevo(profesional, sillon)))
                .satisfies(e -> esSolapado(e, "SILLON"));
        assertThat(jdbc.queryForObject("select count(*) from turno where profesional_id = ?", Integer.class, profesional))
                .isZero();
    }

    @Test
    @DisplayName("La base rechaza un solapamiento por profesional y la API lo informa como PROFESIONAL")
    void baseRechazaPorProfesional() {
        existente(profesional, otroSillon, "2030-06-10T12:45:00Z", "2030-06-10T13:15:00Z", "CONFIRMADO");
        assertThatThrownBy(() -> persistencia.guardar(nuevo(profesional, sillon)))
                .satisfies(e -> esSolapado(e, "PROFESIONAL"));
    }

    @Test
    @DisplayName("Un turno cancelado superpuesto no impide guardar")
    void canceladoNoImpide() {
        existente(profesional, sillon, "2030-06-10T13:00:00Z", "2030-06-10T13:30:00Z", "CANCELADO");
        assertThat(persistencia.guardar(nuevo(profesional, sillon)).id()).isNotNull();
    }

    @Test
    @DisplayName("La consulta de ocupados devuelve solo los no cancelados del profesional o del sillón que solapan")
    void turnosQueSolapan() {
        var delProfesional = existente(profesional, otroSillon, "2030-06-10T12:45:00Z", "2030-06-10T13:15:00Z", "PROGRAMADO");
        var delSillon = existente(otroProfesional, sillon, "2030-06-10T13:15:00Z", "2030-06-10T13:45:00Z", "ATENDIDO");
        existente(profesional, sillon, "2030-06-10T12:30:00Z", "2030-06-10T12:45:00Z", "CANCELADO");
        existente(profesional, otroSillon, "2030-06-10T13:30:00Z", "2030-06-10T14:00:00Z", "PROGRAMADO"); // contiguo
        existente(otroProfesional, tercerSillon, "2030-06-10T13:00:00Z", "2030-06-10T13:15:00Z", "PROGRAMADO"); // ajeno
        existente(profesional, otroSillon, "2030-06-10T12:00:00Z", "2030-06-10T12:30:00Z", "CANCELADO");

        var ocupados = persistencia.turnosQueSolapan(profesional, sillon, Intervalo.desde(DIEZ, 30));

        assertThat(ocupados).extracting(TurnoOcupado::turnoId).containsExactlyInAnyOrder(delProfesional, delSillon);
        assertThat(ocupados).allSatisfy(t -> assertThat(t.estado()).isNotEqualTo(EstadoTurno.CANCELADO));
    }
}
