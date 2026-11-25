package ar.edu.utn.turnos.listaespera.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.Hueco;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link BuscadorCompatibles} con PostgreSQL real y sin usuario autenticado, como lo usará el aviso automático
 * (C-36). El reloj fijo hace que ahora sea 2026-12-01 09:00 en Buenos Aires.
 */
@Transactional
@Import(RelojFijoConfig.class)
class BuscadorCompatiblesIT extends PruebaIntegracionBase {
    /** Jueves 3 de diciembre de 2026, 10:00 en Buenos Aires. */
    private static final Instant INICIO = Instant.parse("2026-12-03T13:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BuscadorCompatibles buscador;

    private Long profesional;
    private Long consulta;
    private Hueco huecoDeTreinta;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        profesional = DatosAgenda.profesional(jdbc, true);
        consulta = prestacionDe(profesional, 30, true);
        huecoDeTreinta = new Hueco(profesional, INICIO, INICIO.plus(Duration.ofMinutes(30)));
    }

    private Long prestacionDe(Long prof, int minutos, boolean activa) {
        var prestacion = DatosAgenda.prestacion(jdbc, minutos, activa);
        DatosAgenda.asignarPrestacion(jdbc, prof, prestacion);
        return prestacion;
    }

    private Long entrada(Long paciente, Long prestacion, String creadoEn) {
        return DatosFicticios.entrada(jdbc, paciente, prestacion, null, "2026-12-01", "2026-12-31", null, null,
                "ACTIVA", creadoEn);
    }

    private Long paciente() {
        return DatosFicticios.paciente(jdbc, "buscador@correo.test");
    }

    @Test
    @DisplayName("El buscador filtra por duración, por prestación realizada y por prestación activa")
    void filtraConLaFuncionPura() {
        var compatible = entrada(paciente(), consulta, "2026-11-01T10:00:00Z");
        entrada(paciente(), prestacionDe(profesional, 90, true), "2026-11-01T10:00:00Z");
        entrada(paciente(), prestacionDe(profesional, 30, false), "2026-11-01T10:00:00Z");
        entrada(paciente(), DatosAgenda.prestacion(jdbc, 30, true), "2026-11-01T10:00:00Z");

        assertThat(buscador.compatibles(huecoDeTreinta)).extracting(EntradaListaEspera::id)
                .containsExactly(compatible);
    }

    @Test
    @DisplayName("El buscador devuelve todas las compatibles y los avisos aplican el tope y un aviso por paciente")
    void avisosConTopeYDeduplicacion() {
        jdbc.update("""
                insert into configuracion (clave, valor, descripcion, actualizado_en)
                values ('lista_espera.max_avisos_por_hueco', '2', 'Tope ficticio', now())
                on conflict (clave) do update set valor = excluded.valor
                """);
        var otraConsulta = prestacionDe(profesional, 20, true);
        var a = paciente();
        var b = paciente();
        var c = paciente();
        var primeraDeA = entrada(a, consulta, "2026-11-01T10:00:00Z");
        var segundaDeA = entrada(a, otraConsulta, "2026-11-02T10:00:00Z");
        var deB = entrada(b, consulta, "2026-11-03T10:00:00Z");
        var deC = entrada(c, consulta, "2026-11-04T10:00:00Z");

        assertThat(buscador.compatibles(huecoDeTreinta)).extracting(EntradaListaEspera::id)
                .containsExactly(primeraDeA, segundaDeA, deB, deC);
        assertThat(buscador.aAvisar(huecoDeTreinta)).extracting(EntradaListaEspera::id)
                .containsExactly(primeraDeA, deB);
    }

    @Test
    @DisplayName("No hay avisos para un hueco que ya empezó")
    void huecoEmpezado() {
        entrada(paciente(), consulta, "2026-11-01T10:00:00Z");
        var ahora = RelojFijoConfig.INSTANTE_FIJO;
        var empezado = new Hueco(profesional, ahora, ahora.plus(Duration.ofMinutes(30)));

        assertThat(buscador.aAvisar(empezado)).isEmpty();
    }
}
