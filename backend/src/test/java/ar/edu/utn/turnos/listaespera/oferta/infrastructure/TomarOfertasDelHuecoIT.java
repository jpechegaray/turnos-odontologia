package ar.edu.utn.turnos.listaespera.oferta.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Toma en bloque de las ofertas de un hueco por inicio y búsqueda del sillón sugerido (C-43, design D4/D6). */
@Transactional
class TomarOfertasDelHuecoIT extends PruebaIntegracionBase {
    private static final Instant INICIO = Instant.parse("2026-12-07T13:00:00Z");

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    OfertasHuecoPersistencia ofertas;

    Long profesional;
    Long otroProfesional;
    Long entradaA;
    Long entradaB;
    Long entradaC;

    @BeforeEach
    void preparar() {
        var prestacion = DatosFicticios.prestacion(jdbc, "Consulta");
        profesional = DatosFicticios.profesional(jdbc, "MN-TO1", true);
        otroProfesional = DatosFicticios.profesional(jdbc, "MN-TO2", true);
        entradaA = entrada("a@tomar.test", prestacion);
        entradaB = entrada("b@tomar.test", prestacion);
        entradaC = entrada("c@tomar.test", prestacion);
    }

    Long entrada(String email, Long prestacion) {
        var paciente = DatosFicticios.paciente(jdbc, email);
        return DatosFicticios.entrada(jdbc, paciente, prestacion, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", "2026-11-01T10:00:00Z");
    }

    Long oferta(Long entrada, Long prof, Instant inicio, Instant fin, Long sillon, String estado) {
        return jdbc.queryForObject("""
                insert into oferta_hueco (lista_espera_id, profesional_id, hueco_inicio, hueco_fin, sillon_sugerido_id,
                                          token_hash, expira_en, estado, creado_en)
                values (?, ?, ?, ?, ?, ?, ?, ?, now()) returning id
                """, Long.class, entrada, prof, java.sql.Timestamp.from(inicio), java.sql.Timestamp.from(fin), sillon,
                OfertaHuecoTablaIT.hashUnico(), java.sql.Timestamp.from(inicio), estado);
    }

    String estado(Long id) {
        return jdbc.queryForObject("select estado from oferta_hueco where id = ?", String.class, id);
    }

    @Test
    @DisplayName("Tomar del hueco solo pasa a TOMADA las ofertas enviadas del mismo profesional e inicio")
    void tomarDelHuecoSoloTomaLasEnviadasDelMismoProfesionalEInicio() {
        var corta = oferta(entradaA, profesional, INICIO, INICIO.plusSeconds(1800), null, "ENVIADA");
        var larga = oferta(entradaB, profesional, INICIO, INICIO.plusSeconds(3600), null, "ENVIADA");
        var vencida = oferta(entradaC, profesional, INICIO, INICIO.plusSeconds(1800), null, "VENCIDA");
        var otroInicio = oferta(entradaA, profesional, INICIO.plusSeconds(3600), INICIO.plusSeconds(5400), null,
                "ENVIADA");
        var otroProf = oferta(entradaB, otroProfesional, INICIO, INICIO.plusSeconds(1800), null, "ENVIADA");

        assertThat(ofertas.tomarDelHueco(profesional, INICIO)).isEqualTo(2);

        assertThat(estado(corta)).isEqualTo("TOMADA");
        assertThat(estado(larga)).isEqualTo("TOMADA");
        assertThat(estado(vencida)).isEqualTo("VENCIDA");
        assertThat(estado(otroInicio)).isEqualTo("ENVIADA");
        assertThat(estado(otroProf)).isEqualTo("ENVIADA");
        assertThat(ofertas.tomarDelHueco(profesional, INICIO)).isZero();
    }

    @Test
    @DisplayName("La oferta sugerida prefiere la de la entrada y si no usa otra del mismo hueco")
    void laOfertaSugeridaPrefiereLaDeLaEntrada() {
        var sillonA = DatosAgenda.sillon(jdbc, true);
        var sillonB = DatosAgenda.sillon(jdbc, true);
        oferta(entradaB, profesional, INICIO, INICIO.plusSeconds(1800), sillonB, "ENVIADA");
        oferta(entradaA, profesional, INICIO, INICIO.plusSeconds(1800), sillonA, "ENVIADA");

        assertThat(ofertas.sugeridaPara(entradaA, profesional, INICIO)).map(OfertaHueco::sillonSugeridoId)
                .contains(sillonA);
        assertThat(ofertas.sugeridaPara(entradaC, profesional, INICIO)).map(OfertaHueco::sillonSugeridoId)
                .isPresent();
        assertThat(ofertas.sugeridaPara(entradaA, otroProfesional, INICIO)).isEmpty();
    }
}
