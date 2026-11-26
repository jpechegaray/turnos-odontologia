package ar.edu.utn.turnos.pacientes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AltaPacienteReservaOnlineTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T13:00:00.123456789Z");

    @Test
    @DisplayName("Reservar con una ficha nueva guarda el consentimiento en la ficha")
    void guardaConsentimientoConElRelojYLaVersion() {
        var repositorio = mock(PacienteRepositorio.class);
        when(repositorio.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        var alta = new AltaPacienteReservaOnlineImpl(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

        Paciente ficha = alta.registrar(new AltaPacienteReservaOnline.DatosFicha("Ana", "Ficticia",
                "ana@example.com", "11 5555-0001", "Obra Social Ficticia"), "2026-11-v1");

        var esperado = Instant.parse("2026-12-01T13:00:00.123456Z");
        assertThat(ficha.consentimientoDatosEn()).isEqualTo(esperado);
        assertThat(ficha.consentimientoVersion()).isEqualTo("2026-11-v1");
        assertThat(ficha.creadoEn()).isEqualTo(esperado);
        assertThat(ficha.dni()).isNull();
        assertThat(ficha.obraSocial()).isEqualTo("Obra Social Ficticia");
    }
}
