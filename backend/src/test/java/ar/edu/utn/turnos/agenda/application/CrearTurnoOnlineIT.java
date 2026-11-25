package ar.edu.utn.turnos.agenda.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.domain.SolicitudTurno;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;

class CrearTurnoOnlineIT extends PruebaIntegracionBase {

    @Autowired
    private CrearTurnoService servicio;

    @Test
    @DisplayName("El alta online de la agenda solo corre dentro de una transacción abierta")
    void crearOnlineSinTransaccionFalla() {
        var solicitud = new SolicitudTurno(1L, 1L, 1L, 1L, Instant.parse("2030-06-10T13:00:00Z"), null);

        assertThatThrownBy(() -> servicio.crearOnline(solicitud))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
