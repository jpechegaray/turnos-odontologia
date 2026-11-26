package ar.edu.utn.turnos.reservapublica.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import ar.edu.utn.turnos.configuracion.application.ConfiguracionServicio;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DisponibilidadPublicaTest {

    static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");
    static final Instant DESDE = Instant.parse("2026-12-01T13:00:00Z");
    static final Instant HASTA = Instant.parse("2026-12-01T15:00:00Z");

    DisponibilidadService nucleo = mock(DisponibilidadService.class);
    ProfesionalConsulta profesionales = mock(ProfesionalConsulta.class);
    ConsultaPrestaciones prestaciones = mock(ConsultaPrestaciones.class);
    ProfesionalRealizaPrestacion asignaciones = mock(ProfesionalRealizaPrestacion.class);
    ConfiguracionServicio configuracion = mock(ConfiguracionServicio.class);
    DisponibilidadPublica fachada = new DisponibilidadPublica(nucleo, profesionales, prestaciones, asignaciones,
            configuracion, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void recursosValidos() {
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(
                new ProfesionalConsulta.Datos(1L, "Luis", "Ficticio", "MP-1", null, true)));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(new PrestacionResumen(2L, "Consulta", 30, true)));
        when(asignaciones.realiza(1L, 2L)).thenReturn(true);
        when(configuracion.anticipacionMinimaReservaOnlineHoras()).thenReturn(2);
        when(configuracion.horizonteReservaOnlineDias()).thenReturn(60);
    }

    @Test
    @DisplayName("El rango pedido se valida antes que los recursos")
    void rangoAntesQueRecursos() {
        assertThatThrownBy(() -> fachada.consultar(99L, 2L, HASTA, DESDE))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("RANGO_DISPONIBILIDAD_INVALIDO");
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });
        verifyNoInteractions(profesionales, nucleo);
    }

    @Test
    @DisplayName("Un recurso no disponible se informa antes del recorte")
    void recursoAntesDelRecorte() {
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(
                new ProfesionalConsulta.Datos(1L, "Luis", "Ficticio", "MP-1", null, false)));

        assertThatThrownBy(() -> fachada.consultar(1L, 2L, Instant.parse("2027-03-01T00:00:00Z"),
                Instant.parse("2027-03-02T00:00:00Z")))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo(DisponibilidadPublica.RECURSO_NO_DISPONIBLE);
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                });
        verifyNoInteractions(nucleo);
    }

    @Test
    @DisplayName("El núcleo recibe el rango recortado y la duración por defecto")
    void recorteYDuracion() {
        var minimo = AHORA.plusSeconds(2 * 3600);
        when(nucleo.consultar(1L, 2L, minimo, HASTA, null)).thenReturn(List.of(
                new HorarioDisponible(minimo, minimo.plusSeconds(1800), 7L)));

        assertThat(fachada.consultar(1L, 2L, DESDE, HASTA))
                .containsExactly(new Intervalo(minimo, minimo.plusSeconds(1800)));
    }

    @Test
    @DisplayName("Sin rango después del recorte no se consulta el núcleo")
    void sinRangoTrasRecorte() {
        assertThat(fachada.consultar(1L, 2L, AHORA, AHORA.plusSeconds(3600))).isEmpty();
        verify(nucleo, never()).consultar(anyLong(), anyLong(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROFESIONAL_INEXISTENTE", "PROFESIONAL_INACTIVO", "PRESTACION_INEXISTENTE",
            "PRESTACION_INACTIVA", "RN-11"})
    @DisplayName("Los códigos de recurso del núcleo se traducen a 404")
    void traduccionDefensiva(String codigo) {
        when(nucleo.consultar(anyLong(), anyLong(), any(), any(), any()))
                .thenThrow(new ReglaNegocioException(codigo, EstadoError.NO_PROCESABLE_422, "detalle interno"));

        assertThatThrownBy(() -> fachada.consultar(1L, 2L, DESDE, HASTA))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo(DisponibilidadPublica.RECURSO_NO_DISPONIBLE);
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                    assertThat(ex.getMessage()).isEqualTo(DisponibilidadPublica.MENSAJE_NO_DISPONIBLE);
                });
    }

    @Test
    @DisplayName("Los demás errores del núcleo no se traducen")
    void otrosErrores() {
        var original = new ReglaNegocioException("RANGO_DISPONIBILIDAD_INVALIDO", EstadoError.NO_PROCESABLE_422, "x");
        when(nucleo.consultar(anyLong(), anyLong(), any(), any(), any())).thenThrow(original);

        assertThatThrownBy(() -> fachada.consultar(1L, 2L, DESDE, HASTA)).isSameAs(original);
    }
}
