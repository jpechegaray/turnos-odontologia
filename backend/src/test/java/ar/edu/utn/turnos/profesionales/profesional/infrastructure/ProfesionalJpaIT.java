package ar.edu.utn.turnos.profesionales.profesional.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.ContenedoresPrueba;
import jakarta.persistence.EntityManager;
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
class ProfesionalJpaIT {

    @Autowired
    private ProfesionalJpaRepository profesionales;

    @Autowired
    private EntityManager entidades;

    @Test
    @DisplayName("PostgreSQL genera el ID y admite especialidad nula")
    void generaIdConEspecialidadNula() {
        var creado = profesionales.saveAndFlush(new Profesional("Ana", "Ficticia", "MP-0001", null, true));
        assertThat(creado.getId()).isPositive();
        entidades.clear();

        assertThat(profesionales.findById(creado.getId())).hasValueSatisfying(profesional -> {
            assertThat(profesional.getMatricula()).isEqualTo("MP-0001");
            assertThat(profesional.getEspecialidad()).isNull();
            assertThat(profesional.isActivo()).isTrue();
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("PostgreSQL impide repetir la matrícula de un profesional activo o inactivo")
    void rechazaMatriculaRepetida(boolean activo) {
        profesionales.saveAndFlush(new Profesional("Ana", "Ficticia", "MP-0002", null, activo));
        var distinta = profesionales.saveAndFlush(
                new Profesional("Bruno", "Inventado", "mp-0002", "Ortodoncia", true));
        assertThat(distinta.getId()).isPositive();

        assertThatThrownBy(() -> profesionales.saveAndFlush(
                new Profesional("Celia", "Imaginaria", " MP-0002 ", null, true)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        error -> assertThat(error.getConstraintName()).isEqualTo("uk_profesional_matricula"));
    }
}
