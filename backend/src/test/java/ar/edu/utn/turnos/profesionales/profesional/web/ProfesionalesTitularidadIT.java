package ar.edu.utn.turnos.profesionales.profesional.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.profesionales.profesional.application.ProfesionalVinculadoConsulta;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(ProfesionalesTitularidadIT.VinculoFicticio.class)
class ProfesionalesTitularidadIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/profesionales";
    private static final String TITULAR = "odontologo-ficticio";
    private static final String MATRICULA_PROPIA = "MP-VINCULO";

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Un odontólogo vinculado solo puede consultar su propio registro")
    void soloLeeSuRegistro() throws Exception {
        Long propio = guardar(MATRICULA_PROPIA, false);
        Long ajeno = guardar("MP-AJENA", true);

        mockMvc.perform(get(RUTA).param("id", ajeno.toString()).with(user(TITULAR).roles("ODONTOLOGO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(propio)).andExpect(jsonPath("$[0].activo").value(false));
        mockMvc.perform(get(RUTA + "/{id}", propio).with(user(TITULAR).roles("ODONTOLOGO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(propio))
                .andExpect(jsonPath("$.matricula").value(MATRICULA_PROPIA));
        for (Long id : new Long[]{ajeno, Long.MAX_VALUE}) {
            mockMvc.perform(get(RUTA + "/{id}", id).with(user(TITULAR).roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.matricula").doesNotExist());
        }
        mockMvc.perform(get(RUTA + "/{id}", propio).with(user("otro-odontologo").roles("ODONTOLOGO")))
                .andExpect(status().isForbidden());
    }

    private Long guardar(String matricula, boolean activo) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo) values (?, ?, ?, ?) returning id
                """, Long.class, "Ana", "Ficticia", matricula, activo);
    }

    @TestConfiguration
    static class VinculoFicticio {

        @Bean
        ProfesionalVinculadoConsulta profesionalVinculado(JdbcTemplate jdbc) {
            return identidad -> TITULAR.equals(identidad)
                    ? jdbc.queryForList("select id from profesional where matricula = ?", Long.class, MATRICULA_PROPIA)
                            .stream().findFirst()
                    : Optional.empty();
        }
    }
}
