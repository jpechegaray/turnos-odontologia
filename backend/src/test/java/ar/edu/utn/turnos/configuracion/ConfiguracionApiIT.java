package ar.edu.utn.turnos.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

class ConfiguracionApiIT extends PruebaIntegracionBase {

    private static final String RUTA = "/api/configuracion";
    private static final String FILAS = "select clave, valor, actualizado_en from configuracion order by clave";
    private static final String PLAZO = "\"cancelacion.plazo_horas\": 24";
    private static final String RETENCION = "\"consultorio.retencion_anios\": 2";
    private static final String DEFAULTS = """
            {
              "cancelacion.plazo_horas": 24,
              "recordatorio.anticipacion_horas": 24,
              "reserva_online.horizonte_dias": 60,
              "reserva_online.anticipacion_minima_horas": 2,
              "reserva_online.max_turnos_activos_por_email": 3,
              "lista_espera.max_avisos_por_hueco": 10,
              "consultorio.nombre": "Consultorio Ficticio",
              "consultorio.domicilio": "Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires",
              "consultorio.email_contacto": "contacto@consultorio-ficticio.example",
              "consultorio.retencion_anios": 2
            }
            """;

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECEPCION"})
    @DisplayName("Admin y Recepción consultan los parámetros vigentes")
    void permiteConsulta(String rol) throws Exception {
        mockMvc.perform(get(RUTA).with(user("personal").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().json(DEFAULTS, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Odontólogo no puede consultar configuración")
    void deniegaOdontologo() throws Exception {
        mockMvc.perform(get(RUTA).with(user("personal").roles("ODONTOLOGO")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("Un cliente anónimo no puede consultar configuración")
    void deniegaAnonimo() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest
    @CsvSource({"0,1,1", "168,168,20"})
    @Transactional
    @DisplayName("Admin reemplaza toda la configuración con límites válidos")
    void reemplazaTodaLaConfiguracion(int plazo, int anticipacion, int retencion) throws Exception {
        var antes = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        jdbc.update("update configuracion set actualizado_en = ?", antes);
        var nuevo = DEFAULTS.replace(PLAZO, "\"cancelacion.plazo_horas\": " + plazo)
                .replace("\"recordatorio.anticipacion_horas\": 24", "\"recordatorio.anticipacion_horas\": " + anticipacion)
                .replace("Consultorio Ficticio", "Consultorio de Prueba")
                .replace("\"consultorio.retencion_anios\": 2", "\"consultorio.retencion_anios\": " + retencion);

        mockMvc.perform(conCsrfValido(escritura("ADMIN", nuevo)))
                .andExpect(status().isOk())
                .andExpect(content().json(nuevo, JsonCompareMode.STRICT));
        mockMvc.perform(get(RUTA).with(user("personal").roles("RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(content().json(nuevo, JsonCompareMode.STRICT));

        var cambiadas = Set.of("cancelacion.plazo_horas", "recordatorio.anticipacion_horas", "consultorio.nombre",
                "consultorio.retencion_anios");
        var fechas = jdbc.query("select clave, actualizado_en from configuracion",
                (rs, fila) -> Map.entry(rs.getString("clave"), rs.getObject("actualizado_en", OffsetDateTime.class)))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(fechas).hasSize(10).containsKeys(cambiadas.toArray(String[]::new));
        var fechaCambio = fechas.get("cancelacion.plazo_horas");
        assertThat(fechaCambio).isAfter(antes);
        fechas.forEach((clave, fecha) -> assertThat(fecha).as(clave)
                .isEqualTo(cambiadas.contains(clave) ? fechaCambio : antes));
    }

    @Test
    @DisplayName("Repetir el mismo reemplazo no cambia las fechas")
    void reemplazoIdenticoNoActualizaFechas() throws Exception {
        var antes = jdbc.queryForList(FILAS);
        mockMvc.perform(conCsrfValido(escritura("ADMIN", DEFAULTS)))
                .andExpect(status().isOk())
                .andExpect(content().json(DEFAULTS, JsonCompareMode.STRICT));
        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEPCION", "ODONTOLOGO"})
    @DisplayName("Recepción y Odontólogo no pueden editar configuración")
    void deniegaEdicionSinRol(String rol) throws Exception {
        var antes = jdbc.queryForList(FILAS);
        mockMvc.perform(conCsrfValido(escritura(rol, DEFAULTS.replace(PLAZO, "\"cancelacion.plazo_horas\": 72"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Admin no puede editar configuración sin CSRF válido")
    void deniegaEdicionSinCsrfValido(boolean tokenIncorrecto) throws Exception {
        var antes = jdbc.queryForList(FILAS);
        var peticion = escritura("ADMIN", DEFAULTS.replace(PLAZO, "\"cancelacion.plazo_horas\": 72"));
        if (tokenIncorrecto) {
            peticion.with(csrfConCabeceraIncorrecta());
        }
        mockMvc.perform(peticion)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    @DisplayName("Un cuerpo que no coincide con el contrato devuelve 400")
    void rechazaEstructuraInvalida(String cuerpo) throws Exception {
        var antes = jdbc.queryForList(FILAS);
        mockMvc.perform(conCsrfValido(escritura("ADMIN", cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    @ParameterizedTest
    @MethodSource("valoresInvalidos")
    @DisplayName("Un valor inválido devuelve RN-36 sin guardar los demás")
    void rechazaIncumplimientoRn36(String cuerpo) throws Exception {
        var antes = jdbc.queryForList(FILAS);
        mockMvc.perform(conCsrfValido(escritura("ADMIN", cuerpo)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("RN-36"));
        assertThat(jdbc.queryForList(FILAS)).isEqualTo(antes);
    }

    private static Stream<String> cuerposInvalidos() {
        return Stream.of("", "{", "[]", "null", DEFAULTS + " {}",
                DEFAULTS.replace(PLAZO + ",", ""),
                DEFAULTS.replace(PLAZO, PLAZO + ", \"zona_horaria\": \"UTC\""),
                DEFAULTS.replace(PLAZO, PLAZO + ", " + PLAZO),
                DEFAULTS.replace(PLAZO, "\"cancelacion.plazo_horas\": null"),
                DEFAULTS.replace(PLAZO, "\"cancelacion.plazo_horas\": \"24\""),
                DEFAULTS.replace("\"Consultorio Ficticio\"", "true"));
    }

    private static Stream<String> valoresInvalidos() {
        var modificado = DEFAULTS.replace("\"lista_espera.max_avisos_por_hueco\": 10",
                "\"lista_espera.max_avisos_por_hueco\": 12");
        return Stream.of(
                modificado.replace(PLAZO, "\"cancelacion.plazo_horas\": 169"),
                modificado.replace(PLAZO, "\"cancelacion.plazo_horas\": -1"),
                modificado.replace(PLAZO, "\"cancelacion.plazo_horas\": 24.00000000000000001"),
                modificado.replace(PLAZO, "\"cancelacion.plazo_horas\": 2147483648"),
                modificado.replace("Consultorio Ficticio", "  "),
                modificado.replace("Consultorio Ficticio", "Consultorio\\u0000Ficticio"),
                modificado.replace(RETENCION, "\"consultorio.retencion_anios\": 0"),
                modificado.replace(RETENCION, "\"consultorio.retencion_anios\": 21"),
                modificado.replace("Av. Ficticia 1234, Ciudad Autónoma de Buenos Aires", " "),
                modificado.replace("contacto@consultorio-ficticio.example", ""));
    }

    private static MockHttpServletRequestBuilder escritura(String rol, String cuerpo) {
        return put(RUTA).with(user("personal").roles(rol)).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private MockHttpServletRequestBuilder conCsrfValido(MockHttpServletRequestBuilder peticion) throws Exception {
        return peticion.with(csrfValido());
    }
}
