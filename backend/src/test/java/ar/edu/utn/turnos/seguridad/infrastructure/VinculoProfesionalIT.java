package ar.edu.utn.turnos.seguridad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.security.Principal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class VinculoProfesionalIT extends PruebaIntegracionBase {

    private static final String CLAVE = "clave-fixture-c13";
    private static final String HASH = new BCryptPasswordEncoder().encode(CLAVE);
    private static final Instant CREADO = Instant.parse("2026-11-22T12:00:00Z");

    @Autowired private ProfesionalVinculadoConsulta vinculos;
    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Un odontólogo vinculado ve solo su propio profesional")
    void odontologoVeSuProfesional() throws Exception {
        long propio = profesional("C13-V-1");
        long ajeno = profesional("C13-V-2");
        usuarios.saveAndFlush(new Usuario("c13-vinculado@consultorio.test", HASH, "Odonto", Rol.ODONTOLOGO, propio, CREADO));

        var sesion = SesionRealPrueba.iniciar(mockMvc, "c13-vinculado@consultorio.test", CLAVE);

        mockMvc.perform(get("/api/profesionales").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propio));
        mockMvc.perform(get("/api/profesionales/" + ajeno).session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una identidad sin vínculo se resuelve sin profesional")
    void identidadSinVinculo() {
        long id = profesional("C13-V-3");
        Usuario recepcion = usuarios.saveAndFlush(new Usuario("c13-recep@consultorio.test", HASH, "Recep", Rol.RECEPCION, CREADO));
        Usuario inactivo = usuarios.saveAndFlush(new Usuario("c13-inactivo@consultorio.test", HASH, "Baja", Rol.ODONTOLOGO, id, CREADO));
        inactivo.desactivar();
        usuarios.flush();

        assertThat(vinculos.buscarIdPorIdentidad(String.valueOf(recepcion.getId()))).isEmpty();
        assertThat(vinculos.buscarIdPorIdentidad(String.valueOf(inactivo.getId()))).isEmpty();
        assertThat(vinculos.buscarIdPorIdentidad("999999")).isEmpty();
        assertThat(vinculos.buscarIdPorIdentidad("no-es-un-id")).isEmpty();
        assertThat(vinculos.buscarIdPorIdentidad(null)).isEmpty();
    }

    @Test
    @DisplayName("La identidad de la sesión se nombra por el id sin cambiar su JSON")
    void identidadComoPrincipal() {
        var identidad = new IdentidadPersonal(42L, "c13@consultorio.test", Rol.ADMIN);

        assertThat(identidad).isInstanceOf(Principal.class);
        assertThat(identidad.getName()).isEqualTo("42");
    }

    private long profesional(String matricula) {
        return jdbc.queryForObject("insert into profesional (nombre, apellido, matricula, activo) values (?, ?, ?, true) returning id",
                Long.class, "Profesional", "Ficticio", matricula);
    }
}
