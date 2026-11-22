package ar.edu.utn.turnos.profesionales.prestacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.prestacion.application.ProfesionalRealizaPrestacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProfesionalRealizaPrestacionIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProfesionalRealizaPrestacion consulta;

    private Long profesional;
    private Long otroProfesional;
    private Long consultaId;
    private Long limpiezaId;

    @BeforeEach
    void preparar() {
        profesional = crearProfesional("MP-REALIZA-1");
        otroProfesional = crearProfesional("MP-REALIZA-2");
        consultaId = crearPrestacion("Consulta Realiza", true);
        limpiezaId = crearPrestacion("Limpieza Realiza", true);
        asociar(profesional, consultaId);
        asociar(otroProfesional, limpiezaId);
    }

    @Test
    @DisplayName("La consulta interna confirma un profesional que realiza la prestación")
    void confirmaAsociacionExistente() {
        assertThat(consulta.realiza(profesional, consultaId)).isTrue();
    }

    @Test
    @DisplayName("La consulta interna niega un profesional que no realiza la prestación")
    void niegaAsociacionInexistente() {
        assertThat(consulta.realiza(profesional, limpiezaId)).isFalse();
    }

    @Test
    @DisplayName("La consulta interna niega ids inexistentes sin error")
    void niegaIdsInexistentesSinError() {
        assertThat(consulta.realiza(-1L, consultaId)).isFalse();
        assertThat(consulta.realiza(profesional, -1L)).isFalse();
        assertThat(consulta.realiza(-1L, -1L)).isFalse();
    }

    @Test
    @DisplayName("La consulta interna no depende de que la prestación esté activa")
    void noDependeDePrestacionActiva() {
        Long blanqueamiento = crearPrestacion("Blanqueamiento Realiza", true);
        asociar(profesional, blanqueamiento);
        jdbc.update("update prestacion set activa = false where id = ?", blanqueamiento);

        assertThat(consulta.realiza(profesional, blanqueamiento)).isTrue();
    }

    private void asociar(Long idProfesional, Long idPrestacion) {
        jdbc.update("insert into profesional_prestacion (profesional_id, prestacion_id) values (?, ?)",
                idProfesional, idPrestacion);
    }

    private Long crearProfesional(String matricula) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo)
                values ('Ana', 'Ficticia', ?, true) returning id
                """, Long.class, matricula);
    }

    private Long crearPrestacion(String nombre, boolean activa) {
        return jdbc.queryForObject("""
                insert into prestacion (nombre, nombre_clave, duracion_default_min, activa)
                values (?, ?, 30, ?) returning id
                """, Long.class, nombre, nombre.toLowerCase(), activa);
    }
}
