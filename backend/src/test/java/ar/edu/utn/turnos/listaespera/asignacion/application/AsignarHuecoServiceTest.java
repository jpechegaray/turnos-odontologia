package ar.edu.utn.turnos.listaespera.asignacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.agenda.application.CrearTurnoService;
import ar.edu.utn.turnos.agenda.application.DisponibilidadService;
import ar.edu.utn.turnos.agenda.domain.CanalTurno;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.agenda.domain.HorarioDisponible;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import ar.edu.utn.turnos.agenda.domain.Turno;
import ar.edu.utn.turnos.agenda.domain.TurnoOcupado;
import ar.edu.utn.turnos.agenda.infrastructure.ConsultaOcupacionDisponibilidad;
import ar.edu.utn.turnos.listaespera.domain.EntradaListaEspera;
import ar.edu.utn.turnos.listaespera.domain.EstadoListaEspera;
import ar.edu.utn.turnos.listaespera.infrastructure.ListaEsperaPersistencia;
import ar.edu.utn.turnos.listaespera.oferta.domain.OfertaHueco;
import ar.edu.utn.turnos.listaespera.oferta.infrastructure.OfertasHuecoPersistencia;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import ar.edu.utn.turnos.profesionales.sillon.application.SillonConsulta;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.tiempo.Intervalo;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsignarHuecoServiceTest {

    private static final Long ENTRADA_ID = 1L;
    private static final Long PACIENTE_ID = 10L;
    private static final Long PRESTACION_ID = 20L;
    private static final Long PROFESIONAL_ID = 3L;
    private static final Long USUARIO_ID = 77L;
    private static final int DURACION = 30;
    private static final Instant INICIO = Instant.parse("2030-06-10T13:00:00Z");
    private static final Instant FIN = INICIO.plus(Duration.ofMinutes(DURACION));
    private static final LocalDate FECHA = LocalDate.of(2030, 6, 10);

    @Mock
    private ListaEsperaPersistencia entradas;

    @Mock
    private OfertasHuecoPersistencia ofertas;

    @Mock
    private ConsultaPrestaciones prestaciones;

    @Mock
    private ProfesionalRealizaPrestacion profesionalPrestaciones;

    @Mock
    private DisponibilidadService disponibilidad;

    @Mock
    private SillonConsulta sillones;

    @Mock
    private ConsultaOcupacionDisponibilidad ocupacion;

    @Mock
    private CrearTurnoService crearTurno;

    @InjectMocks
    private AsignarHuecoService servicio;

    @Test
    @DisplayName("La duración sale de la prestación de la entrada")
    void duracionSaleDeLaPrestacion() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO, FIN, 5L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.empty());
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 5L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().duracionMin()).isNull();
        assertThat(captor.getValue().inicio()).isEqualTo(INICIO);
        assertThat(captor.getValue().sillonId()).isEqualTo(5L);
        verify(disponibilidad).consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null);
    }

    @Test
    @DisplayName("Una entrada incompatible con el hueco responde 422")
    void incompatibleResponde422() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(99L)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);

        assertThatThrownBy(() -> servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("ENTRADA_INCOMPATIBLE");
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });

        verify(crearTurno, never()).crear(any(), any(), any());
        verifyNoInteractions(disponibilidad, sillones, ofertas);
    }

    @Test
    @DisplayName("Recepción asigna un hueco a una entrada compatible")
    void compatibleContinua() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 7L));

        servicio.asignar(ENTRADA_ID, solicitud(7L), USUARIO_ID);

        var entradaCaptor = ArgumentCaptor.forClass(EntradaListaEspera.class);
        verify(entradas).guardar(entradaCaptor.capture());
        assertThat(entradaCaptor.getValue().estado()).isEqualTo(EstadoListaEspera.ASIGNADA);
        assertThat(entradaCaptor.getValue().turnoAsignadoId()).isEqualTo(99L);
        verify(ofertas).tomarDelHueco(PROFESIONAL_ID, INICIO);
    }

    @Test
    @DisplayName("Una entrada que ya no está activa responde 409")
    void noActivaResponde409() {
        var asignada = entradaActiva(PROFESIONAL_ID).asignar(50L);
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(asignada));

        assertThatThrownBy(() -> servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("ENTRADA_NO_ACTIVA");
                    assertThat(error.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                });

        verifyNoInteractions(prestaciones, profesionalPrestaciones, disponibilidad, sillones, ofertas, crearTurno);
    }

    @Test
    @DisplayName("Una entrada inexistente responde 404")
    void inexistenteResponde404() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("ENTRADA_NO_ENCONTRADA");
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                });

        verifyNoInteractions(prestaciones, profesionalPrestaciones, disponibilidad, sillones, ofertas, crearTurno);
    }

    @Test
    @DisplayName("Un sillón indicado se usa tal cual sin consultar disponibilidad")
    void sillonIndicadoSeUsaTalCual() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 7L));

        servicio.asignar(ENTRADA_ID, solicitud(7L), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(7L);
        verifyNoInteractions(disponibilidad, sillones);
        verify(ofertas, never()).sugeridaPara(any(), any(), any());
    }

    @Test
    @DisplayName("Sin sillón se prefiere el sugerido por la oferta si sigue libre")
    void sillonSugeridoLibre() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO, FIN, 8L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.of(oferta(8L)));
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 8L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(8L);
        verifyNoInteractions(sillones);
    }

    @Test
    @DisplayName("El sillón sugerido libre se usa aunque no sea el de menor id")
    void sillonSugeridoLibreAunqueNoSeaElMenor() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO, FIN, 4L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.of(oferta(8L)));
        when(sillones.buscarPorId(8L)).thenReturn(Optional.of(new SillonConsulta.Datos(8L, "Sillón 8", null, true)));
        when(ocupacion.buscar(PROFESIONAL_ID, List.of(8L), new Intervalo(INICIO, FIN))).thenReturn(List.of());
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID))).thenReturn(turno(99L, 8L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("El sillón sugerido ocupado cede al primero activo libre")
    void sillonSugeridoOcupadoCede() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO, FIN, 4L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.of(oferta(8L)));
        when(sillones.buscarPorId(8L)).thenReturn(Optional.of(new SillonConsulta.Datos(8L, "Sillón 8", null, true)));
        when(ocupacion.buscar(PROFESIONAL_ID, List.of(8L), new Intervalo(INICIO, FIN))).thenReturn(List.of(
                new TurnoOcupado(50L, 2L, 8L, new Intervalo(INICIO, FIN), EstadoTurno.PROGRAMADO)));
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID))).thenReturn(turno(99L, 4L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Sin sillón libre sugerido se usa el primero activo libre")
    void sillonLibreNoSugerido() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO, FIN, 6L), new HorarioDisponible(INICIO, FIN, 4L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.empty());
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 4L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(4L);
        verifyNoInteractions(sillones);
    }

    @Test
    @DisplayName("Sin HorarioDisponible en el inicio se usa el sillón de respaldo")
    void sinHorarioEnInicioUsaRespaldo() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null))
                .thenReturn(List.of(new HorarioDisponible(INICIO.plusSeconds(1800), FIN.plusSeconds(1800), 4L)));
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.empty());
        when(sillones.listarActivos()).thenReturn(List.of(new SillonConsulta.Datos(9L, "Sillón 1", null, true)));
        when(crearTurno.crear(any(SolicitudTurno.class), any(), eq(USUARIO_ID)))
                .thenReturn(turno(99L, 9L));

        servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID);

        var captor = ArgumentCaptor.forClass(SolicitudTurno.class);
        verify(crearTurno).crear(captor.capture(), any(), eq(USUARIO_ID));
        assertThat(captor.getValue().sillonId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Sin sillón disponible responde 422")
    void sinSillonDisponible() {
        when(entradas.bloquear(ENTRADA_ID)).thenReturn(Optional.of(entradaActiva(PROFESIONAL_ID)));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion(DURACION, true)));
        when(profesionalPrestaciones.realiza(PROFESIONAL_ID, PRESTACION_ID)).thenReturn(true);
        when(disponibilidad.consultar(PROFESIONAL_ID, PRESTACION_ID, INICIO, FIN, null)).thenReturn(List.of());
        when(ofertas.sugeridaPara(ENTRADA_ID, PROFESIONAL_ID, INICIO)).thenReturn(Optional.empty());
        when(sillones.listarActivos()).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.asignar(ENTRADA_ID, solicitud(null), USUARIO_ID))
                .isInstanceOfSatisfying(ReglaNegocioException.class, error -> {
                    assertThat(error.codigo()).isEqualTo("SIN_SILLON_DISPONIBLE");
                    assertThat(error.estado()).isEqualTo(EstadoError.NO_PROCESABLE_422);
                });

        verify(crearTurno, never()).crear(any(), any(), any());
    }

    private static EntradaListaEspera entradaActiva(Long profesionalId) {
        return new EntradaListaEspera(ENTRADA_ID, PACIENTE_ID, PRESTACION_ID, profesionalId, FECHA, FECHA,
                null, null, EstadoListaEspera.ACTIVA, null, INICIO.minus(Duration.ofHours(1)));
    }

    private static PrestacionResumen prestacion(int duracion, boolean activa) {
        return new PrestacionResumen(PRESTACION_ID, "Limpieza ficticia", duracion, activa);
    }

    private static SolicitudAsignacion solicitud(Long sillonId) {
        return new SolicitudAsignacion(PROFESIONAL_ID, INICIO, sillonId, null);
    }

    private static OfertaHueco oferta(Long sillonSugeridoId) {
        return OfertaHueco.nueva(ENTRADA_ID, PROFESIONAL_ID, INICIO, FIN, sillonSugeridoId, "hash-ficticio",
                INICIO.minus(Duration.ofMinutes(5)));
    }

    private static Turno turno(Long id, Long sillonId) {
        return new Turno(id, PACIENTE_ID, PROFESIONAL_ID, sillonId, PRESTACION_ID, new Intervalo(INICIO, FIN),
                DURACION, EstadoTurno.PROGRAMADO, CanalTurno.INTERNO, null, false, false, null, null, null, null,
                USUARIO_ID, INICIO, null);
    }
}
