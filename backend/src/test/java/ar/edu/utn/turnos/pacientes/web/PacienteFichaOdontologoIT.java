package ar.edu.utn.turnos.pacientes.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Ficha básica para el odontólogo (C-28, RN-33): solo de los pacientes con turnos de su profesional. */
@Transactional
class PacienteFichaOdontologoIT extends PruebaIntegracionBase {

    @Autowired
    private JdbcTemplate jdbc;

    private Long pacientePropio;
    private Long pacienteAjeno;
    private RequestPostProcessor odontologo;

    @BeforeEach
    void preparar() {
        pacientePropio = DatosAgenda.paciente(jdbc, true);
        pacienteAjeno = DatosAgenda.paciente(jdbc, true);
        Long profesional = DatosAgenda.profesional(jdbc, true);
        Long otroProfesional = DatosAgenda.profesional(jdbc, true);
        Long sillon = DatosAgenda.sillon(jdbc, true);
        Long prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.turno(jdbc, pacientePropio, profesional, sillon, prestacion,
                "2026-12-07T10:00:00-03:00", "2026-12-07T10:30:00-03:00", "PROGRAMADO");
        DatosAgenda.turno(jdbc, pacienteAjeno, otroProfesional, sillon, prestacion,
                "2026-12-07T11:00:00-03:00", "2026-12-07T11:30:00-03:00", "PROGRAMADO");
        odontologo = comoOdontologo(profesional);
    }

    private RequestPostProcessor comoOdontologo(Long profesional) {
        String email = "odontologo-ficha-" + System.nanoTime() + "@consultorio.test";
        Long id = jdbc.queryForObject("""
                insert into usuario (email, password_hash, nombre, rol, profesional_id, activo, intentos_fallidos,
                                     creado_en)
                values (?, ?, 'Odontólogo Ficticio', 'ODONTOLOGO', ?, true, 0, now()) returning id
                """, Long.class, email, "$2a$10$" + "x".repeat(53), profesional);
        var identidad = new IdentidadPersonal(id, email, Rol.ODONTOLOGO);
        return authentication(new UsernamePasswordAuthenticationToken(identidad, null,
                List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO"))));
    }

    @Test
    @DisplayName("El odontólogo consulta la ficha de un paciente de sus turnos")
    void fichaPropia() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", pacientePropio).with(odontologo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacientePropio))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Ficticia"));
    }

    @Test
    @DisplayName("El odontólogo no consulta la ficha de un paciente de otro profesional")
    void fichaAjena() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", pacienteAjeno).with(odontologo))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.nombre").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("El odontólogo recibe 403 con un paciente inexistente, sin revelar si existe")
    void fichaInexistente() throws Exception {
        mockMvc.perform(get("/api/pacientes/{id}", Long.MAX_VALUE).with(odontologo))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("El odontólogo sigue sin poder buscar fichas")
    void noBusca() throws Exception {
        mockMvc.perform(get("/api/pacientes").with(odontologo))
                .andExpect(status().isForbidden());
    }
}
