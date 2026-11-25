package ar.edu.utn.turnos.reservapublica.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class TokenTurnoTablaIT extends PruebaIntegracionBase {
    private static final String INICIO = "2030-06-10T13:00:00Z";
    private static final Instant CREADO = Instant.parse("2030-06-09T13:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TokenTurnoRepositorio tokens;

    @Test
    @Transactional
    @DisplayName("La tabla guarda los metadatos del token como instantes e indexa su turno")
    void metadatosEIndice() {
        Long turno = nuevoTurno();
        tokens.saveAndFlush(new TokenTurno(turno, "a".repeat(64), Instant.parse(INICIO), CREADO));

        var fila = jdbc.queryForMap("""
                select turno_id, token_hash, expira_en, creado_en, revocado_en, ultimo_uso_en from token_turno
                where turno_id = ?
                """, turno);
        assertThat(fila).containsEntry("turno_id", turno).containsEntry("token_hash", "a".repeat(64))
                .containsEntry("revocado_en", null).containsEntry("ultimo_uso_en", null);
        assertThat(((java.sql.Timestamp) fila.get("expira_en")).toInstant()).isEqualTo(Instant.parse(INICIO));
        assertThat(((java.sql.Timestamp) fila.get("creado_en")).toInstant()).isEqualTo(CREADO);
        assertThat(jdbc.queryForList("""
                select data_type from information_schema.columns where table_schema = current_schema()
                and table_name = 'token_turno' and column_name in ('expira_en', 'creado_en', 'revocado_en', 'ultimo_uso_en')
                """, String.class)).containsExactly(
                "timestamp with time zone", "timestamp with time zone",
                "timestamp with time zone", "timestamp with time zone");
        assertThat(jdbc.queryForObject("""
                select indexdef from pg_indexes where schemaname = current_schema()
                and tablename = 'token_turno' and indexname = 'ix_token_turno_turno'
                """, String.class)).contains("USING btree (turno_id)");
    }

    @Test
    @Transactional
    @DisplayName("PostgreSQL no admite un hash repetido ni un turno inexistente")
    void restricciones() {
        Long turno = nuevoTurno();
        String hash = "b".repeat(64);
        tokens.saveAndFlush(new TokenTurno(turno, hash, Instant.parse(INICIO), CREADO));
        tokens.saveAndFlush(new TokenTurno(turno, "c".repeat(64), Instant.parse(INICIO), CREADO));
        assertThatThrownBy(() -> tokens.saveAndFlush(new TokenTurno(turno, hash, Instant.parse(INICIO), CREADO)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uq_token_turno_hash");
    }

    @Test
    @Transactional
    @DisplayName("PostgreSQL no admite un hash repetido ni un turno inexistente")
    void referenciaInexistente() {
        assertThatThrownBy(() -> tokens.saveAndFlush(new TokenTurno(-1L, "d".repeat(64), Instant.parse(INICIO), CREADO)))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_token_turno_turno");
    }

    private Long nuevoTurno() {
        return DatosAgenda.turno(jdbc, DatosAgenda.paciente(jdbc, true), DatosAgenda.profesional(jdbc, true),
                DatosAgenda.sillon(jdbc, true), DatosAgenda.prestacion(jdbc, 30, true), INICIO, "2030-06-10T13:30:00Z",
                "PROGRAMADO");
    }
}
