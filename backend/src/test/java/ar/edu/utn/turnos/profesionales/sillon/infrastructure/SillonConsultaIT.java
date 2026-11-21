package ar.edu.utn.turnos.profesionales.sillon.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.ContenedoresPrueba;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ContenedoresPrueba.class, SillonesPersistencia.class})
@ActiveProfiles("test")
class SillonConsultaIT {

    @Autowired private SillonJpaRepository sillones;
    @Autowired private SillonConsulta consulta;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entidades;

    @Test
    @DisplayName("Agenda deja de ofrecer un sillón desactivado")
    void excluyeElDesactivado() {
        Long norte = guardar("Box norte", true);
        Long sur = guardar("Box sur", true);
        assertThat(consulta.listarActivos()).extracting(SillonConsulta.Datos::id).containsExactly(norte, sur);

        cambiarEstado(norte, false);

        assertThat(consulta.listarActivos()).extracting(SillonConsulta.Datos::id).containsExactly(sur);
        assertThat(consulta.buscarPorId(norte)).contains(new SillonConsulta.Datos(norte, "Box norte", null, false));
    }

    @Test
    @DisplayName("Agenda vuelve a ofrecer un sillón reactivado")
    void incluyeElReactivado() {
        Long id = guardar("Box central", false);
        assertThat(consulta.listarActivos()).isEmpty();

        cambiarEstado(id, true);

        assertThat(consulta.listarActivos()).containsExactly(new SillonConsulta.Datos(id, "Box central", null, true));
    }

    @Test
    @DisplayName("Agenda distingue un sillón inactivo de uno inexistente")
    void distingueEstadosDeAusencia() {
        Long activo = guardar("Box norte", true);
        Long inactivo = guardar("Box sur", false);

        assertThat(consulta.buscarPorId(activo)).contains(new SillonConsulta.Datos(activo, "Box norte", null, true));
        assertThat(consulta.buscarPorId(inactivo)).contains(new SillonConsulta.Datos(inactivo, "Box sur", null, false));
        assertThat(consulta.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    private Long guardar(String nombre, boolean activo) {
        return sillones.saveAndFlush(new Sillon(nombre, null, activo)).getId();
    }

    private void cambiarEstado(Long id, boolean activo) {
        assertThat(jdbc.update("update sillon set activo = ? where id = ?", activo, id)).isOne();
        entidades.clear();
    }
}
