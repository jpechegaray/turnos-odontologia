package ar.edu.utn.turnos.pacientes.application;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ar.edu.utn.turnos.PruebaIntegracionBase;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PacienteServicioIT extends PruebaIntegracionBase {

    @Autowired
    private PacienteServicio servicio;

    @Autowired
    private PacienteRepositorio repositorio;

    @Test
    @DisplayName("WHEN crear paciente con DNI duplicado THEN lancza ReglaNegocioException RN-32")
    void crearConDniDuplicado() {
        // GIVEN
        Paciente p1 = Paciente.nuevo("Ana", "Gomez", "11111111", "ana@example.com", "5551111", null, null, null);
        repositorio.guardar(p1);
        Paciente p2 = Paciente.nuevo("Luis", "Perez", "11111111", "luis@example.com", "5552222", null, null, null);
        // WHEN / THEN
        assertThatThrownBy(() -> servicio.crear(p2))
                .isInstanceOf(ReglaNegocioException.class);;
                
    }

    @Test
    @DisplayName("WHEN crear y luego obtener paciente THEN lo devuelve correctamente")
    void crearYObtener() {
        // GIVEN
        Paciente p = Paciente.nuevo("Mario", "Lopez", "22222222", "mario@example.com", "5553333", null, null, null);
        Paciente guardado = servicio.crear(p);
        Long id = guardado.id();
        // WHEN
        Paciente obtenido = servicio.obtener(id);
        // THEN
        assertThat(obtenido).isNotNull();
        assertThat(obtenido.id()).isEqualTo(id);
        assertThat(obtenido.email()).isEqualToIgnoringCase("mario@example.com");
    }

    @Test
    @DisplayName("WHEN actualizar paciente con DNI existente de otro paciente THEN lanza ReglaNegocioException RN-32")
    void actualizarConDniDuplicado() {
        // GIVEN two patients
        Paciente p1 = servicio.crear(Paciente.nuevo("A", "B", "33333333", "a@x.com", "555", null, null, null));
        Paciente p2 = servicio.crear(Paciente.nuevo("C", "D", "44444444", "c@x.com", "555", null, null, null));
        // WHEN trying to set p2 dni to p1's dni
        Paciente nuevoDatos = Paciente.nuevo(p2.nombre(), p2.apellido(), "33333333", p2.email(), p2.telefono(), null, null, null);
        assertThatThrownBy(() -> servicio.actualizar(p2.id(), nuevoDatos))
                .isInstanceOf(ReglaNegocioException.class);;
                
    }

    @Test
    @DisplayName("WHEN editar un paciente con el DNI de una ficha inactiva THEN se permite")
    void actualizarConDniDeFichaInactiva() {
        Paciente inactiva = servicio.crear(Paciente.nuevo("E", "F", "55550001", "e@x.com", "555", null, null, null));
        servicio.darDeBaja(inactiva.id());
        Paciente p = servicio.crear(Paciente.nuevo("G", "H", "55550002", "g@x.com", "555", null, null, null));

        Paciente actualizado = servicio.actualizar(p.id(),
                Paciente.nuevo("G", "H", "55550001", "g@x.com", "555", null, null, null));

        assertThat(actualizado.dni()).isEqualTo("55550001");
    }

    @Test
    @DisplayName("WHEN dar de baja una ficha ya inactiva THEN es idempotente (no lanza excepción)")
    void bajaIdempotente() {
        // GIVEN
        Paciente p = servicio.crear(Paciente.nuevo("E", "F", "55550003", "e@x.com", "555", null, null, null));
        Long id = p.id();
        // first baja
        servicio.darDeBaja(id);
        // second baja should not throw
        assertThatCode(() -> servicio.darDeBaja(id)).doesNotThrowAnyException();
    }
}
