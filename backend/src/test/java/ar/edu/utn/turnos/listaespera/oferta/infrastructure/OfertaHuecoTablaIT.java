package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Restricciones de la tabla {@code oferta_hueco} contra PostgreSQL real (RN-38, design D8). */
@Transactional
class OfertaHuecoTablaIT extends PruebaIntegracionBase {

    @Autowired
    JdbcTemplate jdbc;

    Long entrada;
    Long profesional;

    @BeforeEach
    void preparar() {
        var paciente = DatosFicticios.paciente(jdbc, "tabla@oferta.test");
        var prestacion = DatosFicticios.prestacion(jdbc, "Consulta");
        profesional = DatosFicticios.profesional(jdbc, "MN-OF", true);
        entrada = DatosFicticios.entrada(jdbc, paciente, prestacion, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", "2026-11-01T10:00:00Z");
    }

    int insertar(Long listaEspera, String inicio, String fin) {
        return jdbc.update("""
                insert into oferta_hueco (lista_espera_id, profesional_id, hueco_inicio, hueco_fin, sillon_sugerido_id,
                                          token_hash, expira_en, estado, creado_en)
                values (?, ?, cast(? as timestamptz), cast(? as timestamptz), null, ?, cast(? as timestamptz),
                        'ENVIADA', now())
                """, listaEspera, profesional, inicio, fin, hashUnico(), inicio);
    }

    static String hashUnico() {
        return (UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""));
    }

    @Test
    @DisplayName("La base rechaza una segunda oferta para la misma entrada y el mismo hueco")
    void laBaseRechazaUnaSegundaOfertaParaLaMismaEntradaYElMismoHueco() {
        assertThat(insertar(entrada, "2026-12-07T13:00:00Z", "2026-12-07T13:30:00Z")).isEqualTo(1);

        assertThatThrownBy(() -> insertar(entrada, "2026-12-07T13:00:00Z", "2026-12-07T14:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un hueco vacío o invertido")
    void laBaseRechazaUnHuecoVacioOInvertido() {
        assertThatThrownBy(() -> insertar(entrada, "2026-12-07T13:00:00Z", "2026-12-07T13:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una oferta de una entrada inexistente")
    void laBaseRechazaUnaOfertaDeUnaEntradaInexistente() {
        assertThatThrownBy(() -> insertar(-1L, "2026-12-07T13:00:00Z", "2026-12-07T13:30:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
