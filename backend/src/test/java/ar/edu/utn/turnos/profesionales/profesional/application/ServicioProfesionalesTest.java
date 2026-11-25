package ar.edu.utn.turnos.profesionales.profesional.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta.TurnoEnConflicto;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta.Datos;
import ar.edu.utn.turnos.profesionales.profesional.infrastructure.ProfesionalesPersistencia;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioProfesionalesTest {

    @Mock
    private ProfesionalesPersistencia profesionales;

    @Mock
    private TurnosActivosConsulta turnosActivos;

    private ServicioProfesionales servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioProfesionales(profesionales, Optional.empty(), turnosActivos);
    }

    @Test
    @DisplayName("Desactivar un profesional con turnos futuros activos devuelve 409 RN-37 con recurso PROFESIONAL")
    void rechazaDesactivarConTurnosFuturos() {
        var activo = new Datos(1L, "Ana", "Ficticia", "MP-100", null, true);
        var conflictos = List.of(new TurnoEnConflicto(10L, Instant.parse("2030-06-10T13:00:00Z"),
                Instant.parse("2030-06-10T13:30:00Z"), "CONFIRMADO"));
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(activo));
        when(turnosActivos.futurosDeProfesional(1L)).thenReturn(conflictos);

        assertThatThrownBy(() -> servicio.actualizar(1L, "Ana", "Ficticia", "MP-100", null, false))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(error.codigo()).isEqualTo("RN-37");
                    assertThat(error.propiedades()).containsEntry("recurso", "PROFESIONAL")
                            .containsEntry("turnos", conflictos);
                });

        verify(profesionales, never()).actualizar(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Desactivar un profesional sin turnos futuros activos persiste el cambio")
    void desactivaSinTurnosFuturos() {
        var activo = new Datos(1L, "Ana", "Ficticia", "MP-100", null, true);
        var desactivado = new Datos(1L, "Ana", "Ficticia", "MP-100", null, false);
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(activo));
        when(turnosActivos.futurosDeProfesional(1L)).thenReturn(List.of());
        when(profesionales.actualizar(1L, "Ana", "Ficticia", "MP-100", null, false))
                .thenReturn(Optional.of(desactivado));

        assertThat(servicio.actualizar(1L, "Ana", "Ficticia", "MP-100", null, false)).isEqualTo(desactivado);
        verify(turnosActivos).futurosDeProfesional(1L);
    }

    @Test
    @DisplayName("Editar un profesional ya inactivo manteniendo activo=false no consulta turnos futuros")
    void editarYaInactivoNoConsultaTurnos() {
        var inactivo = new Datos(1L, "Ana", "Ficticia", "MP-100", null, false);
        var editado = new Datos(1L, "Ana", "Ficticia", "MP-101", "Ortodoncia", false);
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(inactivo));
        when(profesionales.actualizar(1L, "Ana", "Ficticia", "MP-101", "Ortodoncia", false))
                .thenReturn(Optional.of(editado));

        assertThat(servicio.actualizar(1L, "Ana", "Ficticia", "MP-101", "Ortodoncia", false)).isEqualTo(editado);
        verifyNoInteractions(turnosActivos);
    }

    @Test
    @DisplayName("Actualizar un profesional inexistente responde 404 sin consultar turnos")
    void rechazaProfesionalInexistente() {
        when(profesionales.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(99L, "Ana", "Ficticia", "MP-999", null, false))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                    assertThat(error.codigo()).isEqualTo("PROFESIONAL_NO_ENCONTRADO");
                });

        verifyNoInteractions(turnosActivos);
    }
}
