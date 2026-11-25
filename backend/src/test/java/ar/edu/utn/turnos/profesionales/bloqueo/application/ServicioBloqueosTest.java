package ar.edu.utn.turnos.profesionales.bloqueo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta.TurnoEnConflicto;
import ar.edu.utn.turnos.profesionales.bloqueo.infrastructure.BloqueosPersistencia;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta.Datos;
import ar.edu.utn.turnos.profesionales.profesional.application.ServicioProfesionales;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioBloqueosTest {

    private static final Instant DIEZ = Instant.parse("2030-06-10T13:00:00Z");
    private static final Intervalo INTERVALO = new Intervalo(DIEZ, DIEZ.plusSeconds(3600));
    private static final Datos PROFESIONAL_ACTIVO = new Datos(1L, "Ana", "Ficticia", "MP-100", null, true);

    @Mock
    private BloqueosPersistencia bloqueos;

    @Mock
    private ProfesionalConsulta profesionales;

    @Mock
    private ServicioProfesionales catalogo;

    @Mock
    private TurnosActivosConsulta turnosActivos;

    @InjectMocks
    private ServicioBloqueos servicio;

    @Test
    @DisplayName("Rechaza crear un bloqueo con 409 RN-17 cuando existen turnos activos en el intervalo")
    void rechazaSiHayTurnosActivosEnIntervalo() {
        var conflictos = List.of(
                new TurnoEnConflicto(10L, DIEZ, DIEZ.plusSeconds(1800), "PROGRAMADO"),
                new TurnoEnConflicto(11L, DIEZ.plusSeconds(1800), DIEZ.plusSeconds(3600), "CONFIRMADO"));
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(PROFESIONAL_ACTIVO));
        when(turnosActivos.enIntervaloDeProfesional(1L, INTERVALO)).thenReturn(conflictos);

        assertThatThrownBy(() -> servicio.crear(1L, INTERVALO, "Congreso"))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(error.codigo()).isEqualTo("RN-17");
                    assertThat(error.propiedades()).containsEntry("turnos", conflictos);
                });

        verify(bloqueos, never()).crear(any(), any(), any());
    }

    @Test
    @DisplayName("Crea el bloqueo cuando no existen turnos activos en conflicto")
    void creaCuandoNoHayTurnosEnConflicto() {
        var esperado = new BloqueoDatos(5L, 1L, INTERVALO.inicio(), INTERVALO.fin(), "Congreso");
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(PROFESIONAL_ACTIVO));
        when(turnosActivos.enIntervaloDeProfesional(1L, INTERVALO)).thenReturn(List.of());
        when(bloqueos.crear(1L, INTERVALO, "Congreso")).thenReturn(esperado);

        var creado = servicio.crear(1L, INTERVALO, "Congreso");

        assertThat(creado).isEqualTo(esperado);
        verify(turnosActivos).enIntervaloDeProfesional(1L, INTERVALO);
        verify(bloqueos).crear(1L, INTERVALO, "Congreso");
    }

    @Test
    @DisplayName("Rechaza crear un bloqueo para un profesional inexistente sin consultar turnos")
    void rechazaProfesionalInexistente() {
        when(profesionales.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(99L, INTERVALO, "Congreso"))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                    assertThat(error.codigo()).isEqualTo("PROFESIONAL_NO_ENCONTRADO");
                });

        verifyNoInteractions(turnosActivos, bloqueos);
    }
}
