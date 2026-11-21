package ar.edu.utn.turnos.prestaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PrestacionIT extends PruebaIntegracionBase {

    @Autowired
    private PrestacionJpaRepository prestaciones;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("Una prestación nueva se guarda activa, con el nombre recortado y su clave")
    void guardaPrestacionActiva() {
        Prestacion guardada = prestaciones.saveAndFlush(nueva(" Limpieza ", 45));

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getNombre()).isEqualTo("Limpieza");
        assertThat(guardada.getDuracionDefaultMin()).isEqualTo(45);
        assertThat(guardada.isActiva()).isTrue();
        assertThat(prestaciones.existsByNombreClave("limpieza")).isTrue();
    }

    @Test
    @DisplayName("La base rechaza dos prestaciones con la misma clave de nombre")
    void baseRechazaNombreDuplicado() {
        prestaciones.saveAndFlush(nueva("Consulta", 30));

        assertThatThrownBy(() -> prestaciones.saveAndFlush(nueva(" CONSULTA ", 30)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza una duración fuera de RN-09 aunque se saltee el dominio")
    void baseRechazaDuracionInvalida() {
        Prestacion guardada = prestaciones.saveAndFlush(nueva("Control", 20));

        assertThatThrownBy(() -> em.createNativeQuery(
                        "UPDATE prestacion SET duracion_default_min = 7 WHERE id = :id")
                .setParameter("id", guardada.getId())
                .executeUpdate())
                .rootCause()
                .hasMessageContaining("prestacion_duracion_rn09");
    }

    private static Prestacion nueva(String nombre, int minutos) {
        return new Prestacion(new NombrePrestacion(nombre), new DuracionPrestacion(minutos));
    }
}
