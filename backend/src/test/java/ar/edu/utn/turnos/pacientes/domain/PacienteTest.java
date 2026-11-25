package ar.edu.utn.turnos.pacientes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacienteTest {

    @Test
    @DisplayName("Alta sin email ni teléfono se rechaza")
    void altaSinEmailNiTelefonoSeRechaza() {
        assertThatThrownBy(() -> Paciente.nuevo(" Juan ", " Perez ", null, "   ", "  ", "Obra Social", "123", "Notas"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El paciente debe tener al menos un medio de contacto")
                .extracting(ex -> ((ReglaNegocioException) ex).codigo(), ex -> ((ReglaNegocioException) ex).estado())
                .containsExactly("RN-32", EstadoError.NO_PROCESABLE_422);
    }

    @Test
    @DisplayName("Un DNI en blanco se trata como no informado")
    void dniEnBlancoSeTrataComoNoInformado() {
        Paciente paciente = Paciente.nuevo("Juan", "Perez", "   ", "juan@example.test", null, null, null, null);
        assertThat(paciente.dni()).isNull();
    }

    @Test
    @DisplayName("Contactos en blanco no cuentan como informados")
    void contactosEnBlancoNoCuentanComoInformados() {
        assertThatThrownBy(() -> Paciente.nuevo("Juan", "Perez", "12345678", "  ", " \t ", null, null, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El paciente debe tener al menos un medio de contacto")
                .extracting(ex -> ((ReglaNegocioException) ex).codigo(), ex -> ((ReglaNegocioException) ex).estado())
                .containsExactly("RN-32", EstadoError.NO_PROCESABLE_422);
    }

    @Test
    @DisplayName("conDatos preserva id, activo, consentimientoDatosEn y creadoEn")
    void conDatosPreservaCamposEspeciales() {
        Instant ahora = Instant.now();
        Paciente original = new Paciente(
                10L, "Juan", "Perez", "1234", "juan@example.test", "111", "OS", "999", "Nota", true, ahora, "2026-11-v1",
                ahora);

        Paciente actualizado = original.conDatos("Carlos", "Gomez", "5678", "carlos@example.test", "222", "OS2", "888", "Nota2");

        assertThat(actualizado.id()).isEqualTo(10L);
        assertThat(actualizado.activo()).isTrue();
        assertThat(actualizado.consentimientoDatosEn()).isEqualTo(ahora);
        assertThat(actualizado.consentimientoVersion()).isEqualTo("2026-11-v1");
        assertThat(actualizado.creadoEn()).isEqualTo(ahora);
        assertThat(actualizado.nombre()).isEqualTo("Carlos");
        assertThat(actualizado.apellido()).isEqualTo("Gomez");
        assertThat(actualizado.dni()).isEqualTo("5678");
    }

    @Test
    @DisplayName("Una ficha de la reserva online nace activa, sin DNI y con el consentimiento y su versión")
    void fichaDeReservaOnlineGuardaConsentimiento() {
        Instant aceptado = Instant.parse("2026-12-01T13:00:00Z");

        Paciente ficha = Paciente.nuevoDeReservaOnline(" Ana ", "Ficticia", "ana@example.com", "11 5555-0001",
                "Obra Social Ficticia", aceptado, "2026-11-v1");

        assertThat(ficha.id()).isNull();
        assertThat(ficha.activo()).isTrue();
        assertThat(ficha.dni()).isNull();
        assertThat(ficha.nombre()).isEqualTo("Ana");
        assertThat(ficha.consentimientoDatosEn()).isEqualTo(aceptado);
        assertThat(ficha.consentimientoVersion()).isEqualTo("2026-11-v1");
        assertThat(ficha.creadoEn()).isEqualTo(aceptado);
    }

    @Test
    @DisplayName("dadoDeBaja deja activo en false")
    void dadoDeBajaDejaActivoEnFalse() {
        Paciente original = Paciente.nuevo("Juan", "Perez", null, "juan@example.test", null, null, null, null);
        Paciente dadoDeBaja = original.dadoDeBaja();

        assertThat(dadoDeBaja.activo()).isFalse();
    }
}
