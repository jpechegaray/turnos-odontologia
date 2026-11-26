package ar.edu.utn.turnos.listaespera.oferta.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * RN-15 y RN-24: confirmaciones simultáneas del mismo hueco. Sin {@code @Transactional}: cada pedido confirma su
 * propia transacción, como en producción. Se repite para que una carrera que pase "por suerte" no esconda una falla.
 */
class TomaOfertaConcurrenciaIT extends EscenarioTomaOfertaIT {
    private final List<Long> usuarios = new ArrayList<>();

    @AfterEach
    void limpiarUsuarios() {
        for (Long usuario : usuarios) {
            // Este @AfterEach corre antes que el de la base: el turno de recepción referencia al usuario.
            jdbc.update("delete from turno where creado_por_usuario_id = ?", usuario);
            jdbc.update("delete from usuario where id = ?", usuario);
        }
        usuarios.clear();
    }

    @RepeatedTest(5)
    @DisplayName("Dos pacientes confirman a la vez y solo uno obtiene el turno")
    void dosConfirmacionesSimultaneas() throws Exception {
        var tokenA = ofertaDisponible();
        var tokenB = ofertaDisponible();
        var largada = new CountDownLatch(1);

        var pedidoA = enHilo(largada, post(RUTA + "/tomar", tokenA).with(EscenarioTomaOfertaIT::ipNueva));
        var pedidoB = enHilo(largada, post(RUTA + "/tomar", tokenB).with(EscenarioTomaOfertaIT::ipNueva));
        largada.countDown();
        var respuestas = List.of(pedidoA.get(30, TimeUnit.SECONDS), pedidoB.get(30, TimeUnit.SECONDS));

        assertThat(respuestas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
        var rechazo = respuestas.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
        assertThat((String) JsonPath.read(rechazo.getContentAsString(), "$.codigo")).isEqualTo("HUECO_YA_TOMADO");
        assertThat(turnosDelProfesional()).isEqualTo(1);
        var estados = List.of(estadoEntrada(entradaDe(tokenA)), estadoEntrada(entradaDe(tokenB)));
        assertThat(estados).containsExactlyInAnyOrder("ASIGNADA", "ACTIVA");
        assertThat(List.of(estadoOferta(tokenA), estadoOferta(tokenB))).containsOnly("TOMADA");
    }

    @RepeatedTest(3)
    @DisplayName("Una toma y un alta simultánea de recepción sobre el mismo hueco no se bloquean entre sí")
    void tomaContraRecepcion() throws Exception {
        var token = ofertaDisponible();
        var usuario = DatosAgenda.usuario(jdbc, "RECEPCION");
        usuarios.add((Long) usuario[0]);
        var sesion = authentication(new UsernamePasswordAuthenticationToken(
                new IdentidadPersonal((Long) usuario[0], (String) usuario[1], Rol.RECEPCION), null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPCION"))));
        var csrf = csrfValido();
        var cuerpo = "{\"pacienteId\":%d,\"profesionalId\":%d,\"sillonId\":%d,\"prestacionId\":%d,\"inicio\":\"%s\"}"
                .formatted(pacienteAjeno, profesional, sillon, consulta, INICIO_HUECO);
        var largada = new CountDownLatch(1);

        var toma = enHilo(largada, post(RUTA + "/tomar", token).with(EscenarioTomaOfertaIT::ipNueva));
        var alta = enHilo(largada, post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(sesion).with(csrf));
        largada.countDown();
        var respuestas = List.of(toma.get(30, TimeUnit.SECONDS), alta.get(30, TimeUnit.SECONDS));

        assertThat(respuestas).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
        assertThat(turnosDelProfesional()).isEqualTo(1);
        if (respuestas.get(0).getStatus() == 409) {
            assertThat((String) JsonPath.read(respuestas.get(0).getContentAsString(), "$.codigo"))
                    .isEqualTo("HUECO_YA_TOMADO");
        }
    }

    private CompletableFuture<MockHttpServletResponse> enHilo(CountDownLatch largada,
            MockHttpServletRequestBuilder pedido) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                largada.await(30, TimeUnit.SECONDS);
                return mockMvc.perform(pedido).andReturn().getResponse();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }
}
