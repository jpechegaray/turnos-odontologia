package ar.edu.utn.turnos.reservapublica.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Contexto propio con un límite bajo: el resto de la suite comparte el contexto con el valor por defecto. */
@TestPropertySource(properties = "app.ratelimit.max-solicitudes-por-ip=2")
class ReservaPublicaLimiteTasaIT extends PruebaIntegracionBase {

    private static final RequestPostProcessor MISMA_IP = solicitud -> {
        solicitud.setRemoteAddr("10.38.255.1");
        return solicitud;
    };

    @Test
    @DisplayName("Se supera el límite de solicitudes")
    void superaLimite() throws Exception {
        mockMvc.perform(get("/api/public/prestaciones").with(MISMA_IP)).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/profesionales").param("prestacionId", "1").with(MISMA_IP))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/disponibilidad").with(MISMA_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
