package ar.edu.utn.turnos.reservapublica.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.reservapublica.reserva.application.SeleccionFicha;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeleccionFichaTest {

    private static Paciente ficha(long id, String telefono) {
        return new Paciente(id, "Ana", "Ficticia", null, "ana@example.com", telefono, null, null, null, true, null,
                null, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("Un email nuevo crea una ficha con los datos de la reserva")
    void sinFichasNoEligeNinguna() {
        assertThat(SeleccionFicha.elegir(List.of(), "1155550001")).isEmpty();
    }

    @Test
    @DisplayName("Con varias fichas del mismo email se elige la que coincide en teléfono")
    void eligeLaQueCoincideEnDigitos() {
        var fichas = List.of(ficha(1, "11 5555-0001"), ficha(2, "(11) 5555-0002"));

        assertThat(SeleccionFicha.elegir(fichas, "+11 5555 0002")).contains(fichas.get(1));
    }

    @Test
    @DisplayName("Con varias fichas del mismo email y ningún teléfono coincidente se elige la más antigua")
    void sinCoincidenciaEligeLaMasAntigua() {
        var fichas = List.of(ficha(1, "1155550001"), ficha(2, "1155550002"));

        assertThat(SeleccionFicha.elegir(fichas, "1199990000")).contains(fichas.get(0));
    }

    @Test
    @DisplayName("Un teléfono nulo o sin dígitos nunca cuenta como coincidencia")
    void telefonosNulosOSinDigitosNoCoinciden() {
        var conNulo = List.of(ficha(1, "1155550001"), ficha(2, null), ficha(3, "sin número"));

        assertThat(SeleccionFicha.elegir(conNulo, null)).contains(conNulo.get(0));
        assertThat(SeleccionFicha.elegir(conNulo, "---")).contains(conNulo.get(0));
        assertThat(SeleccionFicha.elegir(List.of(ficha(1, null), ficha(2, "1155550002")), "11 5555 0002"))
                .hasValueSatisfying(elegida -> assertThat(elegida.id()).isEqualTo(2L));
    }

    @Test
    @DisplayName("Una reserva con una ficha existente no modifica la ficha")
    void unaSolaFichaSeReutiliza() {
        var unica = ficha(7, null);

        assertThat(SeleccionFicha.elegir(List.of(unica), "1155550001")).containsSame(unica);
    }
}
