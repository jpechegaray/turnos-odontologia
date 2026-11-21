package ar.edu.utn.turnos.prestaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.prestaciones.application.ConsultaPrestaciones;
import ar.edu.utn.turnos.prestaciones.application.PrestacionResumen;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CatalogoPrestacionesServicioIT extends PruebaIntegracionBase {

    @Autowired
    private CatalogoPrestaciones catalogo;

    @Autowired
    private ConsultaPrestaciones consulta;

    @Autowired
    private PrestacionJpaRepository repositorio;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El servicio rechaza un nombre repetido con 409 PRESTACION-DUPLICADA")
    void rechazaNombreRepetido() {
        catalogo.crear("Limpieza", 45);

        assertThatThrownBy(() -> catalogo.crear(" limpieza ", 30))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("PRESTACION-DUPLICADA");
                    assertThat(ex.estado()).isEqualTo(EstadoError.CONFLICTO_409);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El servicio permite modificar una prestación conservando su propio nombre")
    void modificaConservandoNombre() {
        PrestacionResumen creada = catalogo.crear("Limpieza", 45);

        PrestacionResumen modificada = catalogo.modificar(creada.id(), "LIMPIEZA", 60, true);

        assertThat(modificada.nombre()).isEqualTo("LIMPIEZA");
        assertThat(modificada.duracionDefaultMin()).isEqualTo(60);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El servicio rechaza modificar con el nombre de otra prestación")
    void rechazaModificarConNombreAjeno() {
        catalogo.crear("Limpieza", 45);
        PrestacionResumen consultaInicial = catalogo.crear("Consulta", 30);

        assertThatThrownBy(() -> catalogo.modificar(consultaInicial.id(), "limpieza", 30, true))
                .isInstanceOfSatisfying(ReglaNegocioException.class,
                        ex -> assertThat(ex.codigo()).isEqualTo("PRESTACION-DUPLICADA"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El servicio responde 404 PRESTACION-NO-ENCONTRADA ante un id inexistente")
    void noEncontrada() {
        assertThatThrownBy(() -> catalogo.obtener(999_999L))
                .isInstanceOfSatisfying(ReglaNegocioException.class, ex -> {
                    assertThat(ex.codigo()).isEqualTo("PRESTACION-NO-ENCONTRADA");
                    assertThat(ex.estado()).isEqualTo(EstadoError.NO_ENCONTRADO_404);
                });
        assertThatThrownBy(() -> catalogo.modificar(999_999L, "X", 30, true))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> catalogo.darDeBaja(999_999L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("La baja es lógica e idempotente")
    void bajaLogicaIdempotente() {
        PrestacionResumen creada = catalogo.crear("Blanqueamiento", 60);

        catalogo.darDeBaja(creada.id());
        catalogo.darDeBaja(creada.id());

        assertThat(repositorio.findById(creada.id())).get()
                .extracting(Prestacion::isActiva).isEqualTo(false);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El listado se ordena por nombre y soloActivas excluye las inactivas")
    void listaOrdenadaYFiltrada() {
        catalogo.crear("Limpieza", 45);
        PrestacionResumen blanqueamiento = catalogo.crear("blanqueamiento", 60);
        catalogo.darDeBaja(blanqueamiento.id());

        assertThat(catalogo.listar(false)).extracting(PrestacionResumen::nombre)
                .containsExactly("blanqueamiento", "Limpieza");
        assertThat(catalogo.listar(true)).extracting(PrestacionResumen::nombre)
                .containsExactly("Limpieza");
    }

    @Test
    @DisplayName("Otro módulo consulta una prestación existente")
    void puertoDevuelvePrestacionExistente() {
        Prestacion guardada = repositorio.saveAndFlush(new Prestacion(
                new ar.edu.utn.turnos.prestaciones.domain.NombrePrestacion("Blanqueamiento"),
                new ar.edu.utn.turnos.prestaciones.domain.DuracionPrestacion(60)));
        guardada.desactivar();
        repositorio.flush();

        assertThat(consulta.buscarPorId(guardada.getId())).hasValueSatisfying(resumen -> {
            assertThat(resumen.nombre()).isEqualTo("Blanqueamiento");
            assertThat(resumen.duracionDefaultMin()).isEqualTo(60);
            assertThat(resumen.activa()).isFalse();
        });
    }

    @Test
    @DisplayName("Otro módulo consulta una prestación inexistente")
    void puertoInformaInexistente() {
        assertThat(consulta.buscarPorId(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("Sin usuario autenticado la escritura se rechaza")
    void escrituraSinUsuario() {
        assertThatThrownBy(() -> catalogo.crear("Consulta", 30))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "RECEPCION")
    @DisplayName("Recepción no puede escribir en el servicio")
    void recepcionNoEscribe() {
        assertThatThrownBy(() -> catalogo.crear("Consulta", 30)).isInstanceOf(AccessDeniedException.class);
        assertThat(repositorio.existsByNombreClave("consulta")).isFalse();
    }

    @Test
    @WithMockUser(roles = "ODONTOLOGO")
    @DisplayName("El odontólogo no puede escribir en el servicio")
    void odontologoNoEscribe() {
        assertThatThrownBy(() -> catalogo.crear("Consulta", 30)).isInstanceOf(AccessDeniedException.class);
    }
}
