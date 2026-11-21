package ar.edu.utn.turnos.shared.tiempo;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConfiguracionRelojIT extends PruebaIntegracionBase {

    @Autowired
    private Clock reloj;

    @Test
    @DisplayName("El reloj del sistema usa la zona de Buenos Aires")
    void relojDelContextoUsaZonaDeBuenosAires() {
        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}
