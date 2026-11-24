package ar.edu.utn.turnos.profesionales.profesional.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ProfesionalesApiIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/profesionales";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    @Test
    @DisplayName("El administrador da de alta un profesional activo")
    void daDeAlta() throws Exception {
        var respuesta = mockMvc.perform(escritura(post(RUTA), "ADMIN", """
                        {"nombre":"Ana","apellido":"Ficticia","matricula":" MP-ALTA "}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Ficticia"))
                .andExpect(jsonPath("$.matricula").value("MP-ALTA"))
                .andExpect(jsonPath("$.especialidad").value(nullValue()))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse();
        Long id = jdbc.queryForObject("select id from profesional where matricula = ?", Long.class, "MP-ALTA");
        assertThat(respuesta.getHeader("Location")).isEqualTo(RUTA + "/" + id);
        assertThat(jdbc.queryForObject("select activo from profesional where id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    @DisplayName("El administrador modifica los datos de un profesional")
    void edita() throws Exception {
        Long id = guardar("MP-EDITAR", true);
        mockMvc.perform(escritura(put(RUTA + "/{id}", id), "ADMIN", """
                        {"nombre":"Bruno","apellido":"Inventado","matricula":" MP-EDITADA ",
                         "especialidad":"Ortodoncia","activo":true}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Bruno"))
                .andExpect(jsonPath("$.apellido").value("Inventado"))
                .andExpect(jsonPath("$.matricula").value("MP-EDITADA"))
                .andExpect(jsonPath("$.especialidad").value("Ortodoncia"));
        assertThat(fila(id)).containsEntry("matricula", "MP-EDITADA").containsEntry("especialidad", "Ortodoncia");
    }

    @Test
    @DisplayName("El catálogo rechaza datos obligatorios inválidos")
    void rechazaObligatoriosInvalidos() throws Exception {
        Long id = guardar("MP-INVALIDA", true);
        var original = fila(id);
        Long cantidad = cantidad();
        for (String campo : new String[]{"nombre", "apellido", "matricula"}) {
            for (Object valor : new Object[]{" ", 123, true, Map.of("texto", "Ana")}) {
                var campos = new HashMap<String, Object>(Map.of(
                        "nombre", "Ana", "apellido", "Ficticia", "matricula", "MP-VALIDA"));
                campos.put(campo, valor);
                invalida(post(RUTA), json.writeValueAsString(campos), campo);
                campos.put("activo", true);
                invalida(put(RUTA + "/{id}", id), json.writeValueAsString(campos), campo);
            }
        }
        invalida(put(RUTA + "/{id}", id), """
                {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-VALIDA"}
                """, "activo");
        for (String valor : new String[]{"\"false\"", "0", "null"}) {
            invalida(put(RUTA + "/{id}", id), """
                    {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-VALIDA","activo":%s}
                    """.formatted(valor), "activo");
        }
        assertThat(cantidad()).isEqualTo(cantidad);
        assertThat(fila(id)).isEqualTo(original);
    }

    @Test
    @DisplayName("Un alta no puede reutilizar la matrícula de un profesional inactivo")
    void rechazaAltaDuplicada() throws Exception {
        Long id = guardar("MP-INACTIVA", false);
        var original = fila(id);
        Long cantidad = cantidad();
        conflicto(post(RUTA), """
                {"nombre":"Bruno","apellido":"Inventado","matricula":" MP-INACTIVA "}
                """);
        assertThat(cantidad()).isEqualTo(cantidad);
        assertThat(fila(id)).isEqualTo(original);
    }

    @Test
    @DisplayName("Una actualización no puede tomar la matrícula de otro profesional")
    void rechazaEdicionDuplicada() throws Exception {
        Long primero = guardar("MP-UNO", true);
        Long segundo = guardar("MP-DOS", false);
        var originalPrimero = fila(primero);
        var originalSegundo = fila(segundo);
        conflicto(put(RUTA + "/{id}", primero), """
                {"nombre":"Bruno","apellido":"Inventado","matricula":"MP-DOS","activo":true}
                """);
        assertThat(fila(primero)).isEqualTo(originalPrimero);
        assertThat(fila(segundo)).isEqualTo(originalSegundo);
    }

    @Test
    @DisplayName("El administrador desactiva y reactiva el mismo registro")
    void cambiaEstadoSinBorrar() throws Exception {
        Long id = guardar("MP-ESTADO", true);
        for (boolean activo : new boolean[]{false, true}) {
            mockMvc.perform(escritura(put(RUTA + "/{id}", id), "ADMIN", """
                            {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-ESTADO","activo":%s}
                            """.formatted(activo)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.activo").value(activo));
            mockMvc.perform(get(RUTA + "/{id}", id).with(user("admin-ficticio").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.activo").value(activo));
            assertThat(fila(id)).containsEntry("activo", activo);
        }
    }

    @Test
    @DisplayName("El catálogo no permite borrar físicamente un profesional")
    void noPermiteBorradoFisico() throws Exception {
        Long id = guardar("MP-NO-BORRAR", true);
        var original = fila(id);
        mockMvc.perform(escritura(delete(RUTA + "/{id}", id), "ADMIN", "{}"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(fila(id)).isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Administración y recepción consultan activos e inactivos")
    void listaAmbosEstados(String rol) throws Exception {
        Long activo = guardar("MP-LISTA-A-" + rol, true);
        Long inactivo = guardar("MP-LISTA-I-" + rol, false);
        mockMvc.perform(get(RUTA).with(user("personal-ficticio").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].activo".formatted(activo), contains(true)))
                .andExpect(jsonPath("$[?(@.id == %d)].activo".formatted(inactivo), contains(false)))
                .andExpect(resultado -> {
                    var ids = new ArrayList<Long>();
                    json.readTree(resultado.getResponse().getContentAsByteArray())
                            .forEach(nodo -> ids.add(nodo.get("id").longValue()));
                    assertThat(ids).isSorted().contains(activo, inactivo);
                });
        for (Long id : new Long[]{activo, inactivo}) {
            mockMvc.perform(get(RUTA + "/{id}", id).with(user("personal-ficticio").roles(rol)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.activo").value(id.equals(activo)));
        }
    }

    @Test
    @DisplayName("Consultar o actualizar un profesional inexistente responde 404")
    void noEncuentraProfesional() throws Exception {
        Long cantidad = cantidad();
        for (String rol : new String[]{"ADMIN", "RECEPCION"}) {
            mockMvc.perform(get(RUTA + "/{id}", Long.MAX_VALUE).with(user("personal-ficticio").roles(rol)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
        }
        mockMvc.perform(escritura(put(RUTA + "/{id}", Long.MAX_VALUE), "ADMIN", """
                        {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-AUSENTE","activo":true}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PROFESIONAL_NO_ENCONTRADO"));
        assertThat(cantidad()).isEqualTo(cantidad);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPCION", "ODONTOLOGO"})
    @DisplayName("Recepción y odontología no pueden escribir en el catálogo")
    void deniegaEscritura(String rol) throws Exception {
        Long id = guardar("MP-SOLO-LECTURA-" + rol, true);
        var original = fila(id);
        Long cantidad = cantidad();
        mockMvc.perform(escritura(post(RUTA), rol, """
                        {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-RECHAZADA"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(escritura(put(RUTA + "/{id}", id), rol, """
                        {"nombre":"Ana","apellido":"Ficticia","matricula":"MP-RECHAZADA","activo":false}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(cantidad()).isEqualTo(cantidad);
        assertThat(fila(id)).isEqualTo(original);
    }

    @Test
    @DisplayName("Una consulta anónima del catálogo requiere autenticación")
    void requiereAutenticacion() throws Exception {
        Long id = guardar("MP-ANONIMO", true);
        for (String ruta : new String[]{RUTA, RUTA + "/" + id}) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    @DisplayName("Un odontólogo sin vínculo no puede leer el catálogo")
    void deniegaLecturaSinVinculo() throws Exception {
        Long id = guardar("MP-SIN-VINCULO", true);
        for (String ruta : new String[]{RUTA, RUTA + "/" + id}) {
            mockMvc.perform(get(ruta).with(user("odontologo-ficticio").roles("ODONTOLOGO")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.matricula").doesNotExist());
        }
    }

    private void invalida(MockHttpServletRequestBuilder peticion, String cuerpo, String campo) throws Exception {
        mockMvc.perform(escritura(peticion, "ADMIN", cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores[0].campo").value(campo));
    }

    private void conflicto(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        mockMvc.perform(escritura(peticion, "ADMIN", cuerpo))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("PROFESIONAL_MATRICULA_DUPLICADA"));
    }

    private Long guardar(String matricula, boolean activo) {
        return jdbc.queryForObject("""
                insert into profesional (nombre, apellido, matricula, activo) values (?, ?, ?, ?) returning id
                """, Long.class, "Ana", "Ficticia", matricula, activo);
    }

    private Map<String, Object> fila(Long id) {
        return jdbc.queryForMap("select * from profesional where id = ?", id);
    }

    private Long cantidad() {
        return jdbc.queryForObject("select count(*) from profesional", Long.class);
    }

    private MockHttpServletRequestBuilder escritura(MockHttpServletRequestBuilder peticion, String rol, String cuerpo)
            throws Exception {
        peticion.with(csrfValido());
        return peticion.with(user("personal-ficticio").roles(rol))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }
}
