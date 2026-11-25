package ar.edu.utn.turnos.prestaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta;
import ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta.TurnoEnConflicto;
import ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion;
import ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CatalogoPrestacionesTest {

    @Mock
    private PrestacionJpaRepository repositorio;

    @Mock
    private TurnosActivosConsulta turnosActivos;

    @InjectMocks
    private CatalogoPrestaciones catalogo;

    @Test
    @DisplayName("Si otra alta gana la carrera por el nombre, el servicio responde 409 PRESTACION-DUPLICADA")
    void traduceCarreraDeUnicidad() {
        when(repositorio.existsByNombreClave("consulta")).thenReturn(false);
        when(repositorio.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> catalogo.crear("Consulta", 30))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        ex -> assertThat(ex.codigo()).isEqualTo("PRESTACION-DUPLICADA"));
    }

    @Test
    @DisplayName("Dar de baja una prestación activa con turnos futuros activos devuelve 409 RN-37")
    void darDeBajaConTurnosFuturos() {
        var prestacion = new Prestacion(new NombrePrestacion("Limpieza"), new DuracionPrestacion(45));
        var conflictos = List.of(new TurnoEnConflicto(15L, Instant.parse("2030-06-10T13:00:00Z"),
                Instant.parse("2030-06-10T13:45:00Z"), "PROGRAMADO"));
        when(repositorio.findById(1L)).thenReturn(Optional.of(prestacion));
        when(turnosActivos.futurosDePrestacion(1L)).thenReturn(conflictos);

        assertThatThrownBy(() -> catalogo.darDeBaja(1L))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(ex.codigo()).isEqualTo("RN-37");
                    assertThat(ex.propiedades()).containsEntry("recurso", "PRESTACION")
                            .containsEntry("turnos", conflictos);
                });
        assertThat(prestacion.isActiva()).isTrue();
    }

    @Test
    @DisplayName("Modificar una prestación pasando de activa a inactiva con turnos futuros devuelve 409 RN-37")
    void modificarDesactivandoConTurnosFuturos() {
        var prestacion = new Prestacion(new NombrePrestacion("Limpieza"), new DuracionPrestacion(45));
        var conflictos = List.of(new TurnoEnConflicto(15L, Instant.parse("2030-06-10T13:00:00Z"),
                Instant.parse("2030-06-10T13:45:00Z"), "CONFIRMADO"));
        when(repositorio.findById(1L)).thenReturn(Optional.of(prestacion));
        when(repositorio.existsByNombreClaveAndIdNot("limpieza", 1L)).thenReturn(false);
        when(turnosActivos.futurosDePrestacion(1L)).thenReturn(conflictos);

        assertThatThrownBy(() -> catalogo.modificar(1L, "Limpieza", 45, false))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(ex.codigo()).isEqualTo("RN-37");
                    assertThat(ex.propiedades()).containsEntry("recurso", "PRESTACION")
                            .containsEntry("turnos", conflictos);
                });
        assertThat(prestacion.isActiva()).isTrue();
    }

    @Test
    @DisplayName("Dar de baja o modificar una prestación ya inactiva no consulta turnos futuros")
    void prestacionYaInactivaNoSeBloquea() {
        var prestacion = new Prestacion(new NombrePrestacion("Limpieza"), new DuracionPrestacion(45));
        prestacion.desactivar();
        when(repositorio.findById(1L)).thenReturn(Optional.of(prestacion));
        when(repositorio.existsByNombreClaveAndIdNot("limpieza profunda", 1L)).thenReturn(false);
        when(repositorio.saveAndFlush(prestacion)).thenReturn(prestacion);

        catalogo.darDeBaja(1L);
        var modificada = catalogo.modificar(1L, "Limpieza profunda", 60, false);

        assertThat(modificada.activa()).isFalse();
        verifyNoInteractions(turnosActivos);
    }
}
