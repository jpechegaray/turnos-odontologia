package ar.edu.utn.turnos.prestaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CatalogoPrestacionesTest {

    @Mock
    private PrestacionJpaRepository repositorio;

    @InjectMocks
    private CatalogoPrestaciones catalogo;

    @Test
    @DisplayName("Si otra alta gana la carrera por el nombre, el servicio responde 409 PRESTACION-DUPLICADA")
    void traduceCarreraDeUnicidad() {
        when(repositorio.existsByNombreClave("consulta")).thenReturn(false);
        when(repositorio.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> catalogo.crear("Consulta", 30))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        ex -> assertThat(ex.codigo()).isEqualTo("PRESTACION-DUPLICADA"));
    }
}
