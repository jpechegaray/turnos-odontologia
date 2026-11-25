package ar.edu.utn.turnos.seguridad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Prueba transversal de RN-33 (C-28): el odontólogo es solo lectura. Recorre los endpoints de escritura
 * registrados en Spring MVC, así un endpoint nuevo queda cubierto sin agregarlo a mano.
 */
@Transactional
class EscriturasOdontologoIT extends PruebaIntegracionBase {
    private static final Set<RequestMethod> ESCRITURAS =
            Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);
    private static final Set<String> EXCLUIDOS = Set.of("/api/auth/login", "/api/auth/logout");
    private static final List<String> TABLAS = List.of("turno", "pacientes", "profesional", "sillon", "prestacion",
            "profesional_prestacion", "horario_atencion", "bloqueo", "lista_espera", "usuario");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private JdbcTemplate jdbc;

    /** Pares "MÉTODO patrón" de escritura bajo /api/, salvo login, logout y la zona pública. */
    private Set<String> endpointsDeEscritura() {
        Set<String> endpoints = new TreeSet<>();
        mappings.getHandlerMethods().keySet().forEach(info -> {
            Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
            for (String patron : info.getPatternValues()) {
                if (!patron.startsWith("/api/") || patron.startsWith("/api/public/") || EXCLUIDOS.contains(patron)) {
                    continue;
                }
                metodos.stream().filter(ESCRITURAS::contains).forEach(m -> endpoints.add(m.name() + " " + patron));
            }
        });
        return endpoints;
    }

    private Map<String, Long> filasPorTabla() {
        Map<String, Long> filas = new LinkedHashMap<>();
        TABLAS.forEach(tabla -> filas.put(tabla, jdbc.queryForObject("select count(*) from " + tabla, Long.class)));
        return filas;
    }

    @Test
    @DisplayName("Toda escritura de un odontólogo responde 403")
    void todaEscrituraResponde403() throws Exception {
        Set<String> endpoints = endpointsDeEscritura();
        assertThat(endpoints).as("la prueba no puede pasar vacía").isNotEmpty().contains("POST /api/turnos");
        Map<String, Long> antes = filasPorTabla();

        List<String> sin403 = new ArrayList<>();
        for (String endpoint : endpoints) {
            String[] partes = endpoint.split(" ", 2);
            String url = partes[1].replaceAll("\\{[^}]+}", "1");
            int estado = mockMvc.perform(request(HttpMethod.valueOf(partes[0]), url)
                            .with(user("odontologo-ficticio").roles("ODONTOLOGO"))
                            .with(csrfValido())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn().getResponse().getStatus();
            if (estado != 403) {
                sin403.add(endpoint + " → " + estado);
            }
        }

        assertThat(sin403).as("endpoints de escritura que no responden 403 al odontólogo").isEmpty();
        assertThat(filasPorTabla()).isEqualTo(antes);
    }
}
