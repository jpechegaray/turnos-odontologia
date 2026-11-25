package ar.edu.utn.turnos.shared.notificacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.shared.notificacion.domain.NotificacionPort;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * El perfil {@code test} levanta el adaptador en memoria, no el de SMTP, y enlaza el bloque
 * {@code app.notificaciones} con el planificador apagado.
 */
class ConfiguracionNotificacionesIT extends PruebaIntegracionBase {

    @Autowired
    private NotificacionPort puerto;

    @Autowired
    private PropiedadesNotificaciones propiedades;

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("Las pruebas usan el adaptador en memoria")
    void lasPruebasUsanElAdaptadorEnMemoria() {
        assertThat(puerto).isInstanceOf(InMemoryNotificacionAdapter.class);
        assertThat(contexto.getBeanNamesForType(SmtpNotificacionAdapter.class)).isEmpty();
    }

    @Test
    @DisplayName("El contexto enlaza las propiedades de app.notificaciones en el perfil test")
    void elContextoEnlazaLasPropiedadesEnElPerfilTest() {
        assertThat(propiedades.canal()).isEqualTo("memoria");
        assertThat(propiedades.planificador().habilitado()).isFalse();
        assertThat(propiedades.reintento().maximoIntentos()).isEqualTo(3);
        assertThat(propiedades.reintento().esperaInicial()).isEqualTo(Duration.ofMinutes(5));
        assertThat(propiedades.reintento().multiplicador()).isEqualTo(2);
    }
}
