package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración del puerto de consulta de Paciente.
 * Verifica que los métodos de consulta funcionen correctamente sobre la base de datos real (Testcontainers).
 */
class PacienteConsultaIT extends PruebaIntegracionBase {

    @Autowired
    private PacienteConsulta consulta;

    @Autowired
    private PacienteRepositorio repositorio;

    @Test
    @DisplayName("La consulta por identificador devuelve la ficha")
    void consultaPorIdentificadorDevuelveFicha() {
        // GIVEN
        Paciente paciente = Paciente.nuevo("Paciente", "Activo", "00000001", "paciente1@example.test", "0000-000001", null, null, null);
        Paciente guardado = repositorio.guardar(paciente);
        Long id = guardado.id();

        // WHEN
        Optional<Paciente> encontrado = consulta.porId(id);

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().id()).isEqualTo(id);
        assertThat(encontrado.get().activo()).isTrue();
    }

    @Test
    @DisplayName("La consulta por email ignora mayúsculas")
    void consultaPorEmailIgnoraMayusculas() {
        // GIVEN
        Paciente paciente = Paciente.nuevo("Paciente", "Email", "00000002", "paciente2@example.test", "0000-000002", null, null, null);
        repositorio.guardar(paciente);
        String emailBuscado = "PACIENTE2@EXAMPLE.TEST";

        // WHEN
        Optional<Paciente> encontrado = consulta.porEmail(emailBuscado);

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().email()).isEqualToIgnoringCase(emailBuscado);
    }

    @Test
    @DisplayName("La consulta por identificador devuelve también una ficha inactiva")
    void consultaPorIdentificadorDevuelveFichaInactiva() {
        // GIVEN
        Paciente paciente = Paciente.nuevo("Paciente", "Inactivo", "00000003", "paciente3@example.test", "0000-000003", null, null, null).dadoDeBaja();
        Paciente guardado = repositorio.guardar(paciente);
        Long id = guardado.id();

        // WHEN
        Optional<Paciente> encontrado = consulta.porId(id);

        // THEN
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().activo()).isFalse();
    }

    @Test
    @DisplayName("La consulta de una ficha inexistente devuelve vacío")
    void consultaFichaInexistenteDevuelveVacio() {
        // GIVEN
        Long inexistente = Long.MAX_VALUE;

        // WHEN
        Optional<Paciente> encontradoPorId = consulta.porId(inexistente);
        Optional<Paciente> encontradoPorEmail = consulta.porEmail("noexiste@example.test");

        // THEN
        assertThat(encontradoPorId).isNotPresent();
        assertThat(encontradoPorEmail).isNotPresent();
    }
}
