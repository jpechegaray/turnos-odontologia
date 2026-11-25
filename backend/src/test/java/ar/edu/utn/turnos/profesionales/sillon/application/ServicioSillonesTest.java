package ar.edu.utn.turnos.profesionales.sillon.application;

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
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta.Datos;
import ar.edu.utn.turnos.profesionales.sillon.infrastructure.SillonesPersistencia;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ServicioSillonesTest {

    @Mock private SillonesPersistencia sillones;
    @Mock private TurnosActivosConsulta turnosActivos;
    @InjectMocks private ServicioSillones servicio;

    @Test
    @DisplayName("Una violación de integridad ajena al nombre se propaga sin traducir")
    void noTraduceOtraRestriccion() {
        var causa = new ConstraintViolationException("Otro error", new SQLException(), "otra_restriccion");
        var original = new DataIntegrityViolationException("Error de integridad", causa);
        when(sillones.crear("Box ficticio", null)).thenThrow(original);

        assertThatThrownBy(() -> servicio.crear("Box ficticio", null)).isSameAs(original);
    }

    @Test
    @DisplayName("La restricción del nombre se traduce al código del catálogo")
    void traduceSoloRestriccionDelNombre() {
        var causa = new ConstraintViolationException("Nombre duplicado", new SQLException(), "uk_sillon_nombre");
        when(sillones.crear("Box ficticio", null)).thenThrow(new DataIntegrityViolationException("Integridad", causa));

        assertThatThrownBy(() -> servicio.crear("Box ficticio", null))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        error -> assertThat(error.codigo()).isEqualTo("SILLON_NOMBRE_DUPLICADO"));
    }

    @Test
    @DisplayName("Desactivar un sillón con turnos futuros activos devuelve 409 RN-37 con recurso SILLON")
    void rechazaDesactivarConTurnosFuturos() {
        var conflictos = List.of(new TurnoEnConflicto(10L, Instant.parse("2030-06-10T13:00:00Z"),
                Instant.parse("2030-06-10T13:30:00Z"), "PROGRAMADO"));
        when(sillones.buscarPorId(1L)).thenReturn(Optional.of(new Datos(1L, "Box ficticio", null, true)));
        when(turnosActivos.futurosDeSillon(1L)).thenReturn(conflictos);

        assertThatThrownBy(() -> servicio.actualizar(1L, "Box ficticio", null, false))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                    assertThat(error.codigo()).isEqualTo("RN-37");
                    assertThat(error.propiedades()).containsEntry("recurso", "SILLON")
                            .containsEntry("turnos", conflictos);
                });

        verify(sillones, never()).actualizar(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Desactivar un sillón sin turnos futuros activos actualiza el registro")
    void desactivaSinTurnosFuturos() {
        var desactivado = new Datos(1L, "Box ficticio", null, false);
        when(sillones.buscarPorId(1L)).thenReturn(Optional.of(new Datos(1L, "Box ficticio", null, true)));
        when(turnosActivos.futurosDeSillon(1L)).thenReturn(List.of());
        when(sillones.actualizar(1L, "Box ficticio", null, false)).thenReturn(Optional.of(desactivado));

        assertThat(servicio.actualizar(1L, "Box ficticio", null, false)).isEqualTo(desactivado);
        verify(turnosActivos).futurosDeSillon(1L);
    }

    @Test
    @DisplayName("Editar un sillón ya inactivo manteniendo activo=false no consulta turnos futuros")
    void editarYaInactivoNoConsultaTurnos() {
        var actualizado = new Datos(1L, "Box renombrado", "Planta alta", false);
        when(sillones.buscarPorId(1L)).thenReturn(Optional.of(new Datos(1L, "Box ficticio", null, false)));
        when(sillones.actualizar(1L, "Box renombrado", "Planta alta", false)).thenReturn(Optional.of(actualizado));

        assertThat(servicio.actualizar(1L, "Box renombrado", "Planta alta", false)).isEqualTo(actualizado);
        verifyNoInteractions(turnosActivos);
    }
}
