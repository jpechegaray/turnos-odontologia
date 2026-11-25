package ar.edu.utn.turnos.listaespera.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** La consulta de candidatas solo reduce el volumen; la regla final es la función pura (design D4). */
@Transactional
class CandidatasListaEsperaIT extends PruebaIntegracionBase {
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 10);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ListaEsperaPersistencia persistencia;

    @Test
    @DisplayName("Las candidatas son activas, del profesional o sin preferido y con la fecha en rango, por antigüedad")
    void candidatas() {
        var prestacion = DatosFicticios.prestacion(jdbc, "Candidatas");
        var profesional = DatosFicticios.profesional(jdbc, "MP-CAND", true);
        var otroProfesional = DatosFicticios.profesional(jdbc, "MP-CAND-OTRO", true);
        var p = new Long[9];
        for (int i = 0; i < p.length; i++) {
            p[i] = DatosFicticios.paciente(jdbc, "candidata" + i + "@correo.test");
        }
        var sinPreferido = DatosFicticios.entrada(jdbc, p[0], prestacion, null, "2026-12-01", "2026-12-31",
                null, null, "ACTIVA", "2026-11-03T10:00:00Z");
        var delProfesional = DatosFicticios.entrada(jdbc, p[1], prestacion, profesional, "2026-12-01",
                "2026-12-31", null, null, "ACTIVA", "2026-11-01T10:00:00Z");
        var empiezaEseDia = DatosFicticios.entrada(jdbc, p[2], prestacion, null, "2026-12-10", "2026-12-31",
                null, null, "ACTIVA", "2026-11-02T10:00:00Z");
        var terminaEseDia = DatosFicticios.entrada(jdbc, p[3], prestacion, null, "2026-12-01", "2026-12-10",
                null, null, "ACTIVA", "2026-11-02T10:00:00Z");
        // Fuera: otro profesional, fechas fuera de rango y estados no activos.
        DatosFicticios.entrada(jdbc, p[4], prestacion, otroProfesional, "2026-12-01", "2026-12-31",
                null, null, "ACTIVA", "2026-10-01T10:00:00Z");
        DatosFicticios.entrada(jdbc, p[5], prestacion, null, "2026-12-11", "2026-12-31",
                null, null, "ACTIVA", "2026-10-01T10:00:00Z");
        DatosFicticios.entrada(jdbc, p[6], prestacion, null, "2026-12-01", "2026-12-09",
                null, null, "ACTIVA", "2026-10-01T10:00:00Z");
        DatosFicticios.entrada(jdbc, p[7], prestacion, null, "2026-12-01", "2026-12-31",
                null, null, "CANCELADA", "2026-10-01T10:00:00Z");
        DatosFicticios.entrada(jdbc, p[8], prestacion, null, "2026-12-01", "2026-12-31",
                null, null, "ASIGNADA", "2026-10-01T10:00:00Z");

        var mios = Set.of(p);
        var ids = persistencia.candidatas(profesional, FECHA).stream()
                .filter(e -> mios.contains(e.pacienteId()))
                .map(EntradaListaEspera::id)
                .toList();

        // Empate de antigüedad entre empiezaEseDia y terminaEseDia: desempata el id.
        assertThat(ids).containsExactly(delProfesional, empiezaEseDia, terminaEseDia, sinPreferido);
    }
}
