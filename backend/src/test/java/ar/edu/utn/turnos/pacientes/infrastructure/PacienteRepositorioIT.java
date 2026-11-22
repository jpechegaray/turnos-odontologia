package ar.edu.utn.turnos.pacientes.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class PacienteRepositorioIT extends PruebaIntegracionBase {

    @Autowired
    private PacienteRepositorio repositorio;

    @Autowired
    private PacienteRepositorioJpa jpaRepository;

    @BeforeEach
    void limpiar() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("La base rechaza un DNI duplicado aunque se saltee la validación de la aplicación")
    void laBaseRechazaUnDniDuplicadoAunqueSeSalteeLaValidacion() {
        Paciente primero = Paciente.nuevo("Paciente", "Uno", "00000001", "paciente1@example.test", null, "Obra Social", "100", "Nota");
        Paciente segundo = Paciente.nuevo("Paciente", "Dos", "00000001", "paciente2@example.test", null, "Obra Social", "101", "Nota");

        repositorio.guardar(primero);

        assertThatThrownBy(() -> repositorio.guardar(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
