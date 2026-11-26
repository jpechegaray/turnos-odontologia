package ar.edu.utn.turnos.pacientes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.pacientes.domain.Paciente;
import ar.edu.utn.turnos.pacientes.domain.PacienteRepositorio;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class PacienteRepositorioIT extends PruebaIntegracionBase {

    @Autowired
    private PacienteRepositorio repositorio;

    @Autowired
    private PacienteRepositorioJpa jpaRepository;

    @BeforeEach
    void limpiar() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("La base rechaza un DNI duplicado aunque se saltee la validación de la aplicación")
    void laBaseRechazaUnDniDuplicadoAunqueSeSalteeLaValidacion() {
        Paciente primero = Paciente.nuevo("Paciente", "Uno", "00000001", "paciente1@example.test", null, "Obra Social", "100", "Nota");
        Paciente segundo = Paciente.nuevo("Paciente", "Dos", "00000001", "paciente2@example.test", null, "Obra Social", "101", "Nota");

        repositorio.guardar(primero);

        assertThatThrownBy(() -> repositorio.guardar(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base acepta una ficha inactiva y una activa con el mismo DNI")
    void laBaseAceptaUnaFichaInactivaYUnaActivaConElMismoDni() {
        Paciente inactiva = repositorio.guardar(
                Paciente.nuevo("Paciente", "Cinco", "00000005", "paciente5@example.test", null, null, null, null));
        repositorio.guardar(inactiva.dadoDeBaja());

        Paciente activa = repositorio.guardar(
                Paciente.nuevo("Paciente", "Seis", "00000005", "paciente6@example.test", null, null, null, null));

        assertThat(activa.id()).isNotNull();
        assertThat(jpaRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("La base rechaza dos fichas activas con el mismo DNI aunque exista una inactiva")
    void laBaseRechazaDosActivasConElMismoDniAunqueExistaUnaInactiva() {
        Paciente inactiva = repositorio.guardar(
                Paciente.nuevo("Paciente", "Siete", "00000007", "paciente7@example.test", null, null, null, null));
        repositorio.guardar(inactiva.dadoDeBaja());
        repositorio.guardar(
                Paciente.nuevo("Paciente", "Ocho", "00000007", "paciente8@example.test", null, null, null, null));

        assertThatThrownBy(() -> repositorio.guardar(
                Paciente.nuevo("Paciente", "Nueve", "00000007", "paciente9@example.test", null, null, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Solo una ficha activa cuenta para saber si un DNI está ocupado")
    void soloUnaFichaActivaCuentaParaSaberSiUnDniEstaOcupado() {
        Paciente p = repositorio.guardar(
                Paciente.nuevo("Paciente", "Diez", "00000010", "paciente10@example.test", null, null, null, null));
        assertThat(repositorio.existePorDni("00000010")).isTrue();

        repositorio.guardar(p.dadoDeBaja());

        assertThat(repositorio.existePorDni("00000010")).isFalse();
    }

    @Test
    @DisplayName("La ficha conserva número de afiliado, notas, creación y consentimiento nulo")
    void laFichaConservaDatosComplementarios() {
        Instant creada = Instant.parse("2026-01-02T03:04:05Z");
        Paciente guardado = repositorio.guardar(
                Paciente.nuevo("Paciente", "Tres", null, "paciente3@example.test", null, "Obra Social Ficticia", "AF-3", "Nota ficticia")
                        .conCreacion(creada));

        Paciente leido = repositorio.porId(guardado.id()).orElseThrow();

        assertThat(leido.nroAfiliado()).isEqualTo("AF-3");
        assertThat(leido.notas()).isEqualTo("Nota ficticia");
        assertThat(leido.creadoEn()).isEqualTo(creada);
        assertThat(leido.consentimientoDatosEn()).isNull();
    }

    @Test
    @DisplayName("El consentimiento de datos se persiste cuando se informa")
    void elConsentimientoSePersisteCuandoSeInforma() {
        Instant consentimiento = Instant.parse("2026-02-03T04:05:06Z");
        Paciente base = Paciente.nuevo("Paciente", "Cuatro", null, "paciente4@example.test", null, null, null, null);
        Paciente conConsentimiento = new Paciente(null, base.nombre(), base.apellido(), null, base.email(), null,
                null, null, null, true, consentimiento, "2026-11-v1", Instant.parse("2026-02-03T04:05:06Z"));

        Paciente guardado = repositorio.guardar(conConsentimiento);

        var leido = repositorio.porId(guardado.id()).orElseThrow();
        assertThat(leido.consentimientoDatosEn()).isEqualTo(consentimiento);
        assertThat(leido.consentimientoVersion()).isEqualTo("2026-11-v1");
    }

    @Test
    @DisplayName("Las fichas activas de un email se buscan sin distinguir mayúsculas ni espacios y por orden de alta")
    void activosPorEmailIgnoraMayusculasEspaciosYBajas() {
        var primera = repositorio.guardar(Paciente.nuevo("Ana", "Uno", null, "Ana.Ficticia@Example.com", null, null,
                null, null));
        var baja = repositorio.guardar(Paciente.nuevo("Ana", "Baja", null, "ana.ficticia@example.com", null, null,
                null, null).dadoDeBaja());
        var segunda = repositorio.guardar(Paciente.nuevo("Ana", "Dos", null, "ana.ficticia@example.com", "1155550002",
                null, null, null));
        repositorio.guardar(Paciente.nuevo("Otra", "Persona", null, "otra@example.com", null, null, null, null));

        var encontradas = repositorio.activosPorEmail("  ANA.ficticia@example.COM ");

        assertThat(encontradas).extracting(Paciente::id).containsExactly(primera.id(), segunda.id())
                .doesNotContain(baja.id());
        assertThat(repositorio.activosPorEmail(null)).isEmpty();
        assertThat(repositorio.activosPorEmail("nadie@example.com")).isEmpty();
    }
}
