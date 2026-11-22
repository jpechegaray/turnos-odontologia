package ar.edu.utn.turnos.profesionales.profesional.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.ContenedoresPrueba;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ContenedoresPrueba.class, ProfesionalesPersistencia.class})
@ActiveProfiles("test")
class ProfesionalConsultaIT {

    @Autowired private ProfesionalJpaRepository profesionales;
    @Autowired private ProfesionalConsulta consulta;

    @Test
    @DisplayName("La consulta interna de candidatos excluye inactivos")
    void listaSoloActivosPorId() {
        guardar("MP-0003", false);
        assertThat(consulta.listarActivos()).isEmpty();

        Long primero = guardar("MP-0002", true);
        Long segundo = guardar("MP-0001", true);
        assertThat(consulta.listarActivos()).extracting(ProfesionalConsulta.Datos::id)
                .containsExactly(primero, segundo);
    }

    @Test
    @DisplayName("La consulta interna por ID distingue inactivo de inexistente")
    void distingueAmbosEstadosDeAusencia() {
        Long activo = guardar("MP-0001", true);
        Long inactivo = guardar("MP-0002", false);

        assertThat(consulta.buscarPorId(activo))
                .contains(new ProfesionalConsulta.Datos(activo, "Ana", "Ficticia", "MP-0001", null, true));
        assertThat(consulta.buscarPorId(inactivo))
                .contains(new ProfesionalConsulta.Datos(inactivo, "Ana", "Ficticia", "MP-0002", null, false));
        assertThat(consulta.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    private Long guardar(String matricula, boolean activo) {
        return profesionales.saveAndFlush(new Profesional("Ana", "Ficticia", matricula, null, activo)).getId();
    }
}
