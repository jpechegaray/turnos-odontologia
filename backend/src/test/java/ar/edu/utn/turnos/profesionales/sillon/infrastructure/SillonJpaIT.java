package ar.edu.utn.turnos.profesionales.sillon.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.ContenedoresPrueba;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ContenedoresPrueba.class)
@ActiveProfiles("test")
class SillonJpaIT {

    @Autowired
    private SillonJpaRepository sillones;

    @Test
    @DisplayName("PostgreSQL genera el ID y admite nombres distintos")
    void generaIdYPermiteNombresDistintos() {
        var primero = sillones.saveAndFlush(new Sillon("Box norte", null, true));
        var segundo = sillones.saveAndFlush(new Sillon("BOX NORTE", "Consultorio ficticio", false));

        assertThat(primero.getId()).isPositive();
        assertThat(segundo.getId()).isPositive().isNotEqualTo(primero.getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("PostgreSQL impide repetir el nombre de un sillón activo o inactivo")
    void impideDuplicarNombre(boolean activo) {
        sillones.saveAndFlush(new Sillon("Box central", null, activo));

        assertThatThrownBy(() -> sillones.saveAndFlush(new Sillon("Box central", null, true)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        error -> assertThat(error.getConstraintName()).isEqualTo("uk_sillon_nombre"));
    }
}
