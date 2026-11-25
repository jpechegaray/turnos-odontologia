package ar.edu.utn.turnos.reservapublica.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoints públicos de reserva (C-38). Reloj fijo: martes 1/12/2026 09:00 de Buenos Aires; con los
 * valores por defecto la anticipación mínima es 11:00 de ese día y el horizonte el 30/01/2027 09:00.
 */
@Transactional
@Import(RelojFijoConfig.class)
class ReservaPublicaApiIT extends PruebaIntegracionBase {

    static final String DISPONIBILIDAD = "/api/public/disponibilidad";
    static final String NO_EXISTE = "9223372036854775807";

    /** Cada solicitud sale de una IP distinta para no consumir el límite de tasa del contexto compartido. */
    private static final AtomicInteger IPS = new AtomicInteger();

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    Long paciente;
    Long profesional;
    Long sillon;
    Long prestacion;

    @BeforeEach
    void preparar() {
        paciente = DatosAgenda.paciente(jdbc, true);
        profesional = DatosAgenda.profesional(jdbc, true);
        sillon = DatosAgenda.sillon(jdbc, true);
        prestacion = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacion);
        // Lunes 7/12/2026 de 09:00 a 11:00 de Buenos Aires (12:00Z a 14:00Z).
        DatosAgenda.horario(jdbc, profesional, 1, "09:00", "11:00");
    }

    ResultActions pedir(MockHttpServletRequestBuilder pedido) throws Exception {
        int n = IPS.incrementAndGet();
        return mockMvc.perform(pedido.with(solicitud -> {
            solicitud.setRemoteAddr("10.38." + (n / 250) + "." + (n % 250 + 1));
            return solicitud;
        }));
    }

    MockHttpServletRequestBuilder disponibilidad(Long profesionalId, Long prestacionId, String desde, String hasta) {
        return get(DISPONIBILIDAD).param("profesionalId", profesionalId.toString())
                .param("prestacionId", prestacionId.toString()).param("desde", desde).param("hasta", hasta);
    }

    MockHttpServletRequestBuilder lunes() {
        return disponibilidad(profesional, prestacion, "2026-12-07T09:00:00-03:00", "2026-12-07T11:00:00-03:00");
    }

    JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    List<String> inicios(ResultActions resultado) throws Exception {
        var lista = new ArrayList<String>();
        cuerpo(resultado).forEach(horario -> lista.add(horario.get("inicio").asText()));
        return lista;
    }

    JsonNode elementoConId(JsonNode arreglo, Long id) {
        for (var elemento : arreglo) {
            if (elemento.get("id").asLong() == id) {
                return elemento;
            }
        }
        return null;
    }

    // ---------- Prestaciones reservables públicas ----------

    @Test
    @DisplayName("Se listan solo prestaciones activas con profesional activo")
    void prestacionesReservables() throws Exception {
        var inactiva = DatosAgenda.prestacion(jdbc, 30, false);
        DatosAgenda.asignarPrestacion(jdbc, profesional, inactiva);
        var sinProfesional = DatosAgenda.prestacion(jdbc, 30, true);

        var arreglo = cuerpo(pedir(get("/api/public/prestaciones")).andExpect(status().isOk()));

        var reservable = elementoConId(arreglo, prestacion);
        assertThat(reservable).isNotNull();
        assertThat(reservable.size()).isEqualTo(3);
        assertThat(reservable.get("nombre").asText()).startsWith("Prestación ");
        assertThat(reservable.get("duracionMin").asInt()).isEqualTo(30);
        assertThat(elementoConId(arreglo, inactiva)).isNull();
        assertThat(elementoConId(arreglo, sinProfesional)).isNull();
    }

    @Test
    @DisplayName("Un profesional inactivo no hace reservable una prestación")
    void prestacionConProfesionalInactivo() throws Exception {
        var soloInactivo = DatosAgenda.prestacion(jdbc, 30, true);
        DatosAgenda.asignarPrestacion(jdbc, DatosAgenda.profesional(jdbc, false), soloInactivo);

        var arreglo = cuerpo(pedir(get("/api/public/prestaciones")).andExpect(status().isOk()));

        assertThat(elementoConId(arreglo, soloInactivo)).isNull();
    }

    // ---------- Profesionales reservables por prestación ----------

    @Test
    @DisplayName("Se listan profesionales activos de la prestación sin matrícula")
    void profesionalesReservables() throws Exception {
        DatosAgenda.asignarPrestacion(jdbc, DatosAgenda.profesional(jdbc, false), prestacion);

        var arreglo = cuerpo(pedir(get("/api/public/profesionales").param("prestacionId", prestacion.toString()))
                .andExpect(status().isOk()));

        assertThat(arreglo.size()).isEqualTo(1);
        var unico = arreglo.get(0);
        assertThat(unico.get("id").asLong()).isEqualTo(profesional);
        assertThat(unico.get("nombre").asText()).isEqualTo("Luis");
        assertThat(unico.get("apellido").asText()).isEqualTo("Ficticio");
        assertThat(unico.has("especialidad")).isTrue();
        assertThat(unico.has("matricula")).isFalse();
        assertThat(unico.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Una prestación inexistente o inactiva devuelve lista vacía")
    void profesionalesDePrestacionNoReservable() throws Exception {
        var inactiva = DatosAgenda.prestacion(jdbc, 30, false);
        DatosAgenda.asignarPrestacion(jdbc, profesional, inactiva);

        pedir(get("/api/public/profesionales").param("prestacionId", NO_EXISTE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        pedir(get("/api/public/profesionales").param("prestacionId", inactiva.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Falta el parámetro")
    void profesionalesSinParametro() throws Exception {
        pedir(get("/api/public/profesionales")).andExpect(status().isBadRequest());
        pedir(get("/api/public/profesionales").param("prestacionId", "abc")).andExpect(status().isBadRequest());
    }

    // ---------- Disponibilidad pública ----------

    @Test
    @DisplayName("Un horario ocupado no se ofrece")
    void horarioOcupado() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, "2026-12-07T12:30:00Z",
                "2026-12-07T13:00:00Z", "PROGRAMADO");

        var ofrecidos = inicios(pedir(lunes()).andExpect(status().isOk()));

        assertThat(ofrecidos).doesNotContain("2026-12-07T12:05:00Z", "2026-12-07T12:30:00Z", "2026-12-07T12:55:00Z");
    }

    @Test
    @DisplayName("Un horario contiguo a un turno sí se ofrece")
    void horarioContiguo() throws Exception {
        DatosAgenda.turno(jdbc, paciente, profesional, sillon, prestacion, "2026-12-07T12:30:00Z",
                "2026-12-07T13:00:00Z", "PROGRAMADO");

        var ofrecidos = inicios(pedir(lunes()).andExpect(status().isOk()));

        assertThat(ofrecidos).contains("2026-12-07T12:00:00Z", "2026-12-07T13:00:00Z");
    }

    @Test
    @DisplayName("El JSON tiene solo inicio y fin")
    void soloInicioYFin() throws Exception {
        var resultado = pedir(lunes()).andExpect(status().isOk());

        var arreglo = cuerpo(resultado);
        assertThat(arreglo.size()).isGreaterThan(0);
        arreglo.forEach(horario -> {
            assertThat(horario.size()).isEqualTo(2);
            assertThat(horario.has("inicio")).isTrue();
            assertThat(horario.has("fin")).isTrue();
        });
        assertThat(arreglo.get(0).get("inicio").asText()).isEqualTo("2026-12-07T12:00:00Z");
        assertThat(arreglo.get(0).get("fin").asText()).isEqualTo("2026-12-07T12:30:00Z");
        assertThat(resultado.andReturn().getResponse().getContentAsString()).doesNotContain("sillon");
    }

    @Test
    @DisplayName("Parámetros ausentes o mal formados")
    void parametrosInvalidos() throws Exception {
        pedir(get(DISPONIBILIDAD)).andExpect(status().isBadRequest());
        pedir(get(DISPONIBILIDAD).param("profesionalId", profesional.toString())
                .param("prestacionId", prestacion.toString()).param("desde", "2026-12-07T09:00:00")
                .param("hasta", "2026-12-07T11:00:00-03:00")).andExpect(status().isBadRequest());
        pedir(get(DISPONIBILIDAD).param("profesionalId", "x").param("prestacionId", prestacion.toString())
                .param("desde", "2026-12-07T09:00:00-03:00").param("hasta", "2026-12-07T11:00:00-03:00"))
                .andExpect(status().isBadRequest());
        pedir(get(DISPONIBILIDAD).param("profesionalId", profesional.toString())
                .param("prestacionId", prestacion.toString()).param("desde", "2026-12-07T09:00:00-03:00"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Rango pedido ----------

    @Test
    @DisplayName("Fin no posterior al inicio")
    void finNoPosterior() throws Exception {
        pedir(disponibilidad(profesional, prestacion, "2026-12-07T09:00:00-03:00", "2026-12-07T09:00:00-03:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_DISPONIBILIDAD_INVALIDO"));
        pedir(disponibilidad(profesional, prestacion, "2026-12-07T09:00:00-03:00", "2026-12-07T08:00:00-03:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_DISPONIBILIDAD_INVALIDO"));
    }

    @Test
    @DisplayName("Rango de más de 31 días")
    void rangoLargo() throws Exception {
        pedir(disponibilidad(profesional, prestacion, "2026-12-07T09:00:00-03:00", "2027-01-07T09:00:00.001-03:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RANGO_DISPONIBILIDAD_INVALIDO"));
    }

    @Test
    @DisplayName("Rango válido fuera del horizonte no es un error de rango")
    void fueraDelHorizonte() throws Exception {
        pedir(disponibilidad(profesional, prestacion, "2027-03-01T00:00:00-03:00", "2027-03-08T00:00:00-03:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- Límites de la reserva online ----------

    @Test
    @DisplayName("Borde de anticipación mínima")
    void bordeAnticipacion() throws Exception {
        // Martes 1/12/2026 de 10:00 a 12:00; ahora son las 09:00 y la anticipación mínima es de 2 horas.
        DatosAgenda.horario(jdbc, profesional, 2, "10:00", "12:00");

        var ofrecidos = inicios(pedir(disponibilidad(profesional, prestacion, "2026-12-01T10:00:00-03:00",
                "2026-12-01T12:00:00-03:00")).andExpect(status().isOk()));

        assertThat(ofrecidos).first().isEqualTo("2026-12-01T14:00:00Z");
        assertThat(ofrecidos).doesNotContain("2026-12-01T13:55:00Z").hasSize(7);
    }

    @Test
    @DisplayName("Borde del horizonte")
    void bordeHorizonte() throws Exception {
        // Sábado 30/01/2027 de 08:00 a 10:00; el horizonte de 60 días cae a las 09:00 de ese día.
        DatosAgenda.horario(jdbc, profesional, 6, "08:00", "10:00");

        var arreglo = cuerpo(pedir(disponibilidad(profesional, prestacion, "2027-01-30T08:00:00-03:00",
                "2027-01-30T10:00:00-03:00")).andExpect(status().isOk()));

        assertThat(arreglo.size()).isEqualTo(7);
        assertThat(arreglo.get(0).get("inicio").asText()).isEqualTo("2027-01-30T11:00:00Z");
        assertThat(arreglo.get(6).get("fin").asText()).isEqualTo("2027-01-30T12:00:00Z");
    }

    @Test
    @DisplayName("Un cambio de configuración aplica a la siguiente consulta")
    void cambioDeConfiguracion() throws Exception {
        DatosAgenda.horario(jdbc, profesional, 2, "10:00", "12:00");
        var martes = disponibilidad(profesional, prestacion, "2026-12-01T10:00:00-03:00", "2026-12-01T12:00:00-03:00");
        assertThat(inicios(pedir(martes).andExpect(status().isOk()))).first().isEqualTo("2026-12-01T14:00:00Z");

        jdbc.update("update configuracion set valor = '1' where clave = 'reserva_online.anticipacion_minima_horas'");
        jdbc.update("update configuracion set valor = '5' where clave = 'reserva_online.horizonte_dias'");

        assertThat(inicios(pedir(martes).andExpect(status().isOk()))).first().isEqualTo("2026-12-01T13:00:00Z");
        pedir(lunes()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- Recurso no disponible indistinguible ----------

    @Test
    @DisplayName("Los tres casos dan la misma respuesta")
    void mismoNotFound() throws Exception {
        var profesionalInactivo = DatosAgenda.profesional(jdbc, false);
        DatosAgenda.asignarPrestacion(jdbc, profesionalInactivo, prestacion);
        var prestacionInactiva = DatosAgenda.prestacion(jdbc, 30, false);
        DatosAgenda.asignarPrestacion(jdbc, profesional, prestacionInactiva);
        var sinPrestacion = DatosAgenda.profesional(jdbc, true);
        var desde = "2026-12-07T09:00:00-03:00";
        var hasta = "2026-12-07T11:00:00-03:00";
        var pedidos = List.of(
                get(DISPONIBILIDAD).param("profesionalId", NO_EXISTE).param("prestacionId", prestacion.toString())
                        .param("desde", desde).param("hasta", hasta),
                disponibilidad(profesionalInactivo, prestacion, desde, hasta),
                get(DISPONIBILIDAD).param("profesionalId", profesional.toString()).param("prestacionId", NO_EXISTE)
                        .param("desde", desde).param("hasta", hasta),
                disponibilidad(profesional, prestacionInactiva, desde, hasta),
                disponibilidad(sinPrestacion, prestacion, desde, hasta));

        var cuerpos = new ArrayList<JsonNode>();
        for (var pedido : pedidos) {
            var problema = (ObjectNode) cuerpo(pedir(pedido).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("RECURSO_NO_DISPONIBLE")));
            problema.remove("instance");
            cuerpos.add(problema);
        }

        assertThat(cuerpos).allSatisfy(problema -> assertThat(problema).isEqualTo(cuerpos.get(0)));
    }

    @Test
    @DisplayName("Un profesional inactivo con rango fuera del horizonte da 404")
    void inactivoFueraDelHorizonte() throws Exception {
        var profesionalInactivo = DatosAgenda.profesional(jdbc, false);
        DatosAgenda.asignarPrestacion(jdbc, profesionalInactivo, prestacion);

        pedir(disponibilidad(profesionalInactivo, prestacion, "2027-03-01T00:00:00-03:00", "2027-03-08T00:00:00-03:00"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RECURSO_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("La consulta del personal conserva sus errores detallados")
    void personalConservaErrores() throws Exception {
        var usuario = DatosAgenda.usuario(jdbc, Rol.RECEPCION.name());
        var identidad = new IdentidadPersonal((Long) usuario[0], (String) usuario[1], Rol.RECEPCION);
        var recepcion = authentication(new UsernamePasswordAuthenticationToken(identidad, null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPCION"))));
        var profesionalInactivo = DatosAgenda.profesional(jdbc, false);
        DatosAgenda.asignarPrestacion(jdbc, profesionalInactivo, prestacion);
        var sinPrestacion = DatosAgenda.profesional(jdbc, true);

        mockMvc.perform(get("/api/agenda/disponibilidad").param("profesionalId", profesionalInactivo.toString())
                .param("prestacionId", prestacion.toString()).param("desde", "2026-12-07T09:00:00-03:00")
                .param("hasta", "2026-12-07T11:00:00-03:00").with(recepcion))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("PROFESIONAL_INACTIVO"));
        mockMvc.perform(get("/api/agenda/disponibilidad").param("profesionalId", sinPrestacion.toString())
                .param("prestacionId", prestacion.toString()).param("desde", "2026-12-07T09:00:00-03:00")
                .param("hasta", "2026-12-07T11:00:00-03:00").with(recepcion))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("RN-11"));
    }
}
