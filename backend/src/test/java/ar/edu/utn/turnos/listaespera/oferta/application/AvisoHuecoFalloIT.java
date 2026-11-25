package ar.edu.utn.turnos.listaespera.oferta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Un fallo inesperado al crear las ofertas no hace fallar la cancelación (C-36). Clase aparte porque reemplaza
 * {@link OfertasHueco} por un doble, lo que arma un contexto propio.
 */
@Import(RelojFijoConfig.class)
class AvisoHuecoFalloIT extends PruebaIntegracionBase {

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    OfertasHueco ofertas;

    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;
    Long recepcionista;

    @AfterEach
    void limpiar() {
        DatosAgenda.limpiar(jdbc, paciente, new Long[] {profesional}, new Long[] {sillon}, new Long[] {prestacion});
        jdbc.update("delete from usuario where id = ?", recepcionista);
    }

    @Test
    @DisplayName("Un fallo al avisar no hace fallar la cancelación")
    void unFalloAlAvisarNoHaceFallarLaCancelacion() throws Exception {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        recepcionista = (Long) usuario[0];
        var inicio = RelojFijoConfig.INSTANTE_FIJO.plus(Duration.ofHours(72));
        var turno = DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, inicio.toString(),
                inicio.plus(Duration.ofMinutes(30)).toString(), "PROGRAMADO");
        when(ofertas.crearYAvisar(any(), any(), any(), any())).thenThrow(new IllegalStateException("fallo simulado"));
        var comoRecepcion = authentication(new UsernamePasswordAuthenticationToken(
                new IdentidadPersonal(recepcionista, (String) usuario[1], Rol.RECEPCION), null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPCION"))));

        mockMvc.perform(post("/api/turnos/{id}/cancelar", turno).with(comoRecepcion).with(csrfValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        verify(ofertas).crearYAvisar(any(), any(), any(), any());
        assertThat(jdbc.queryForObject("select estado from turno where id = ?", String.class, turno))
                .isEqualTo("CANCELADO");
    }
}
