package ar.edu.utn.turnos.listaespera.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Sin {@code @Transactional} de clase: varias pruebas necesitan ver violaciones de restricciones en
 * sentencias separadas. Por eso {@link #limpiar()} borra lo que se confirmó, para no ensuciar los
 * listados que prueban otros módulos.
 */
class ListaEsperaJpaIT extends PruebaIntegracionBase {
    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ListaEsperaPersistencia persistencia;

    private Long paciente;
    private Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosFicticios.paciente(jdbc, "jpa-lista@correo.test");
        prestacion = DatosFicticios.prestacion(jdbc, "Consulta JPA lista");
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from lista_espera where paciente_id = ?", paciente);
        jdbc.update("delete from pacientes where id = ?", paciente);
        jdbc.update("delete from prestacion where id = ?", prestacion);
    }

    @Test
    @DisplayName("PostgreSQL guarda fechas como date y la franja como time")
    void tiposDeColumna() {
        var tipos = jdbc.queryForList("""
                select column_name || ':' || data_type from information_schema.columns
                where table_schema = current_schema() and table_name = 'lista_espera'
                  and column_name in ('fecha_desde', 'fecha_hasta', 'franja_desde', 'franja_hasta', 'creado_en')
                order by column_name
                """, String.class);
        assertThat(tipos).containsExactly("creado_en:timestamp with time zone", "fecha_desde:date", "fecha_hasta:date",
                "franja_desde:time without time zone", "franja_hasta:time without time zone");
    }

    @Test
    @Transactional
    @DisplayName("La entidad guarda fechas y franja sin desplazamiento de zona")
    void guardaSinDesplazamiento() {
        var guardada = persistencia.guardar(EntradaListaEspera.nueva(paciente, prestacion, null, DESDE,
                LocalDate.of(2026, 12, 31), LocalTime.of(9, 0), LocalTime.of(12, 30),
                Instant.parse("2026-12-01T12:00:00Z")));
        var fila = jdbc.queryForMap("""
                select fecha_desde::text d, fecha_hasta::text h, franja_desde::text fd, franja_hasta::text fh, estado
                from lista_espera where id = ?
                """, guardada.id());
        assertThat(fila).containsEntry("d", "2026-12-01").containsEntry("h", "2026-12-31")
                .containsEntry("fd", "09:00:00").containsEntry("fh", "12:30:00").containsEntry("estado", "ACTIVA");
        assertThat(persistencia.buscar(guardada.id())).contains(guardada);
    }

    @Test
    @DisplayName("PostgreSQL rechaza rangos y franjas inválidos y referencias inexistentes")
    void rechazaDatosInvalidos() {
        var invalidos = List.of(
                new Object[] {paciente, prestacion, "2026-12-10", "2026-12-09", null, null, "ck_lista_espera_fechas"},
                new Object[] {paciente, prestacion, "2026-12-10", "2026-12-10", "10:00", "10:00", "ck_lista_espera_franja"},
                new Object[] {paciente, prestacion, "2026-12-10", "2026-12-10", "10:00", null, "ck_lista_espera_franja"},
                new Object[] {-1L, prestacion, "2026-12-10", "2026-12-10", null, null, "fk_lista_espera_paciente"},
                new Object[] {paciente, -1L, "2026-12-10", "2026-12-10", null, null, "fk_lista_espera_prestacion"});
        for (Object[] caso : invalidos) {
            assertThatThrownBy(() -> insertar((Long) caso[0], (Long) caso[1], (String) caso[2], (String) caso[3],
                    (String) caso[4], (String) caso[5], "ACTIVA"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining((String) caso[6]);
        }
        assertThatThrownBy(() -> jdbc.update("""
                insert into lista_espera (paciente_id, prestacion_id, profesional_id, fecha_desde, fecha_hasta, estado, creado_en)
                values (?, ?, -1, date '2026-12-10', date '2026-12-10', 'ACTIVA', now())
                """, paciente, prestacion))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_lista_espera_profesional");
    }

    @Test
    @DisplayName("El índice parcial impide dos entradas ACTIVA y permite una CANCELADA más una ACTIVA")
    void indiceParcialDeActivas() {
        insertar(paciente, prestacion, "2026-12-01", "2026-12-31", null, null, "CANCELADA");
        insertar(paciente, prestacion, "2026-12-01", "2026-12-31", null, null, "ACTIVA");
        assertThatThrownBy(() -> insertar(paciente, prestacion, "2026-12-01", "2026-12-31", null, null, "ACTIVA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_lista_espera_activa");
    }

    @Test
    @DisplayName("El script puede ejecutarse dos veces sin duplicar FK ni índice de lista de espera")
    void scriptIdempotente() {
        var script = new ResourceDatabasePopulator(new ClassPathResource("schema-constraints.sql"));
        script.setSeparator("^^^");
        script.execute(dataSource);
        script.execute(dataSource);
        assertThat(jdbc.queryForObject("""
                select count(*) from pg_constraint
                where conrelid = 'lista_espera'::regclass and conname like 'fk_lista_espera_%'
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = 'ux_lista_espera_activa'", Integer.class))
                .isEqualTo(1);
    }

    private void insertar(Long pac, Long pre, String desde, String hasta, String fDesde, String fHasta, String estado) {
        jdbc.update("""
                insert into lista_espera (paciente_id, prestacion_id, fecha_desde, fecha_hasta, franja_desde, franja_hasta,
                                          estado, creado_en)
                values (?, ?, cast(? as date), cast(? as date), cast(? as time), cast(? as time), ?, now())
                """, pac, pre, desde, hasta, fDesde, fHasta, estado);
    }
}
