package ar.edu.utn.turnos.profesionales.sillon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.profesionales.sillon.infrastructure.SillonesPersistencia;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import java.sql.SQLException;
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
}
