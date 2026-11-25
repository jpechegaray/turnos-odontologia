package ar.edu.utn.turnos.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Tabla {@code turno} y sus restricciones en PostgreSQL real (RN-01, RN-02, RN-10, RN-15). Los turnos se
 * insertan por SQL, sin la aplicación: así se prueba la última barrera por sí sola. Sin {@code @Transactional}
 * de clase, porque cada violación tiene que ocurrir en su propia sentencia; {@link #limpiar()} borra lo creado.
 */
class TurnoTablaIT extends PruebaIntegracionBase {
    private static final String DIEZ = "2030-06-10T13:00:00Z";
    private static final String DIEZ_Y_CUARTO = "2030-06-10T13:15:00Z";
    private static final String DIEZ_Y_MEDIA = "2030-06-10T13:30:00Z";
    private static final String DIEZ_Y_CUARENTA_Y_CINCO = "2030-06-10T13:45:00Z";
    private static final String ONCE = "2030-06-10T14:00:00Z";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private Long paciente;
    private Long profesional;
    private Long otroProfesional;
    private Long sillon;
    private Long otroSillon;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        otroProfesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        otroSillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
    }

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional, otroProfesional},
                new Long[] {sillon, otroSillon}, new Long[] {prestacion});
    }

    private Long turno(Long prof, Long sil, String inicio, String fin, String estado) {
        return DatosAgenda.turno(jdbc, paciente, prof, sil, prestacion, inicio, fin, estado);
    }

    @Test
    @DisplayName("Las restricciones de exclusión existen en la base")
    void restriccionesDeExclusionExisten() {
        var nombres = jdbc.queryForList("""
                select conname from pg_constraint
                where conrelid = 'turno'::regclass and contype = 'x'
                order by conname
                """, String.class);
        assertThat(nombres).containsExactly("turno_sin_solape_profesional", "turno_sin_solape_sillon");
    }

    @Test
    @DisplayName("El script de restricciones se puede ejecutar dos veces sin duplicar las de turno")
    void scriptIdempotente() {
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema-constraints.sql"));
        script.setSeparator("^^^");
        assertThatCode(() -> {
            script.execute(dataSource);
            script.execute(dataSource);
        }).doesNotThrowAnyException();
        assertThat(jdbc.queryForObject("""
                select count(*) from pg_constraint where conrelid = 'turno'::regclass
                  and conname in ('turno_sin_solape_profesional', 'turno_sin_solape_sillon',
                                  'fk_turno_paciente', 'fk_turno_profesional', 'fk_turno_sillon',
                                  'fk_turno_prestacion', 'fk_turno_usuario', 'fk_turno_cancelado_por_usuario')
                """, Integer.class)).isEqualTo(8);
    }

    @Test
    @DisplayName("La base rechaza una cancelación atribuida a un usuario inexistente")
    void fkUsuarioQueCancela() {
        var id = turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "PROGRAMADO");

        assertThatThrownBy(() -> jdbc.update("""
                update turno set estado = 'CANCELADO', cancelado_por = 'USUARIO', cancelado_por_usuario_id = -1
                where id = ?
                """, id)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("PostgreSQL guarda los instantes del turno como timestamptz")
    void tiposDeColumna() {
        var tipos = jdbc.queryForList("""
                select column_name || ':' || data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'turno'
                  and column_name in ('inicio', 'fin', 'creado_en', 'actualizado_en', 'cancelado_en')
                order by column_name
                """, String.class);
        assertThat(tipos).containsExactly("actualizado_en:timestamp with time zone",
                "cancelado_en:timestamp with time zone", "creado_en:timestamp with time zone",
                "fin:timestamp with time zone", "inicio:timestamp with time zone");
    }

    @Test
    @DisplayName("PostgreSQL rechaza un turno solapado por profesional aunque sea en otro sillón")
    void excluyeSolapePorProfesional() {
        turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "PROGRAMADO");
        assertThatThrownBy(() -> turno(profesional, otroSillon, DIEZ_Y_CUARTO, DIEZ_Y_CUARENTA_Y_CINCO, "PROGRAMADO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("turno_sin_solape_profesional");
    }

    @Test
    @DisplayName("PostgreSQL rechaza un turno solapado por sillón aunque sea de otro profesional")
    void excluyeSolapePorSillon() {
        turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "CONFIRMADO");
        assertThatThrownBy(() -> turno(otroProfesional, sillon, DIEZ_Y_CUARTO, DIEZ_Y_CUARENTA_Y_CINCO, "PROGRAMADO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("turno_sin_solape_sillon");
    }

    @Test
    @DisplayName("PostgreSQL acepta un turno que empieza justo cuando termina otro (rango [inicio, fin))")
    void aceptaBordeContiguo() {
        turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "PROGRAMADO");
        assertThatCode(() -> turno(profesional, sillon, DIEZ_Y_MEDIA, ONCE, "PROGRAMADO")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PostgreSQL ignora los turnos cancelados y sigue contando los atendidos")
    void canceladosNoOcupan() {
        turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "CANCELADO");
        assertThatCode(() -> turno(profesional, sillon, DIEZ, DIEZ_Y_MEDIA, "PROGRAMADO")).doesNotThrowAnyException();
        turno(otroProfesional, otroSillon, ONCE, "2030-06-10T14:30:00Z", "ATENDIDO");
        assertThatThrownBy(() -> turno(otroProfesional, otroSillon, ONCE, "2030-06-10T14:30:00Z", "PROGRAMADO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("PostgreSQL rechaza intervalos invertidos, duraciones no positivas y referencias inexistentes")
    void rechazaDatosInvalidos() {
        // Duración positiva a propósito, para que salte la regla del intervalo y no la de duración.
        assertThatThrownBy(() -> jdbc.update("""
                insert into turno (paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, duracion_min,
                                   estado, canal, cancelacion_tardia, reprogramado_tarde, creado_en, version)
                values (?, ?, ?, ?, timestamptz '2030-06-10T13:30:00Z', timestamptz '2030-06-10T13:00:00Z', 30,
                        'PROGRAMADO', 'INTERNO', false, false, now(), 0)
                """, paciente, profesional, sillon, prestacion))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_turno_intervalo");
        assertThatThrownBy(() -> jdbc.update("""
                insert into turno (paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, duracion_min,
                                   estado, canal, cancelacion_tardia, reprogramado_tarde, creado_en, version)
                values (?, ?, ?, ?, timestamptz '2030-06-10T13:00:00Z', timestamptz '2030-06-10T13:30:00Z', 0,
                        'PROGRAMADO', 'INTERNO', false, false, now(), 0)
                """, paciente, profesional, sillon, prestacion))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_turno_duracion");
        assertThatThrownBy(() -> DatosAgenda.turno(jdbc, -1L, profesional, sillon, prestacion, DIEZ, DIEZ_Y_MEDIA,
                "PROGRAMADO")).hasMessageContaining("fk_turno_paciente");
        assertThatThrownBy(() -> DatosAgenda.turno(jdbc, paciente, -1L, sillon, prestacion, DIEZ, DIEZ_Y_MEDIA,
                "PROGRAMADO")).hasMessageContaining("fk_turno_profesional");
        assertThatThrownBy(() -> DatosAgenda.turno(jdbc, paciente, profesional, -1L, prestacion, DIEZ, DIEZ_Y_MEDIA,
                "PROGRAMADO")).hasMessageContaining("fk_turno_sillon");
        assertThatThrownBy(() -> DatosAgenda.turno(jdbc, paciente, profesional, sillon, -1L, DIEZ, DIEZ_Y_MEDIA,
                "PROGRAMADO")).hasMessageContaining("fk_turno_prestacion");
    }
}
