package ar.edu.utn.turnos.listaespera.oferta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.PruebaIntegracionBase;
import ar.edu.utn.turnos.agenda.DatosAgenda;
import ar.edu.utn.turnos.agenda.application.CancelarTurnoService;
import ar.edu.utn.turnos.agenda.domain.ActorCancelacion;
import ar.edu.utn.turnos.listaespera.infrastructure.DatosFicticios;
import ar.edu.utn.turnos.seguridad.application.IdentidadPersonal;
import ar.edu.utn.turnos.seguridad.domain.Rol;
import ar.edu.utn.turnos.shared.notificacion.domain.MensajeNotificacion;
import ar.edu.utn.turnos.shared.notificacion.infrastructure.InMemoryNotificacionAdapter;
import ar.edu.utn.turnos.shared.tiempo.RelojFijoConfig;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aviso automático al cancelar un turno (C-36, US-012, RN-06, RN-23, RN-38) de punta a punta con PostgreSQL real.
 * Sin {@code @Transactional}: el oyente corre después del commit. En el perfil {@code test} el aviso es
 * sincrónico, así que al volver la cancelación las ofertas y los emails ya están. El reloj fijo es el martes
 * 1/12/2026 a las 09:00 de Buenos Aires; el turno a cancelar es el viernes 4/12 a las 09:00.
 */
@ExtendWith(OutputCaptureExtension.class)
class AvisoHuecoLiberadoIT extends EscenarioOfertasIT {

    // --- 3.4 ---

    @Test
    @DisplayName("Tras cancelar solo reciben aviso las entradas compatibles")
    void trasCancelarSoloRecibenAvisoLasEntradasCompatibles() throws Exception {
        var deConsulta = paciente("consulta@oferta.test");
        var deConducto = paciente("conducto@oferta.test");
        var entradaConsulta = entrada(deConsulta, consulta, "2026-11-01T10:00:00Z");
        entrada(deConducto, conducto, "2026-11-02T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(entradasConOferta()).containsExactly(entradaConsulta);
        assertThat(destinatarios()).containsExactly(email(deConsulta));
    }

    @Test
    @DisplayName("El tope de avisos elige a las entradas más antiguas")
    void elTopeDeAvisosEligeALasEntradasMasAntiguas() throws Exception {
        jdbc.update("update configuracion set valor = '2' where clave = 'lista_espera.max_avisos_por_hueco'");
        var a = paciente("a@oferta.test");
        var b = paciente("b@oferta.test");
        var c = paciente("c@oferta.test");
        var masNueva = entrada(c, consulta, "2026-11-03T10:00:00Z");
        var masVieja = entrada(a, consulta, "2026-11-01T10:00:00Z");
        var intermedia = entrada(b, consulta, "2026-11-02T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(entradasConOferta()).containsExactlyInAnyOrder(masVieja, intermedia).doesNotContain(masNueva);
        assertThat(destinatarios()).containsExactlyInAnyOrder(email(a), email(b));
    }

    @Test
    @DisplayName("Un paciente con dos entradas compatibles recibe un solo aviso")
    void unPacienteConDosEntradasCompatiblesRecibeUnSoloAviso() throws Exception {
        var otraConsulta = DatosAgenda.prestacion(jdbc, 30, true);
        otrasPrestaciones.add(otraConsulta);
        DatosAgenda.asignarPrestacion(jdbc, profesional, otraConsulta);
        var paciente = paciente("doble@oferta.test");
        var vieja = entrada(paciente, consulta, "2026-11-01T10:00:00Z");
        entrada(paciente, otraConsulta, "2026-11-05T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(entradasConOferta()).containsExactly(vieja);
        assertThat(destinatarios()).containsExactly(email(paciente));
    }

    @Test
    @DisplayName("Sin entradas compatibles no se avisa a nadie")
    void sinEntradasCompatiblesNoSeAvisaANadie() throws Exception {
        entrada(paciente("conducto@oferta.test"), conducto, "2026-11-01T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(ofertas()).isEmpty();
        assertThat(correo.intentos()).isZero();
    }

    @Test
    @DisplayName("Una cancelación revertida no avisa")
    void unaCancelacionRevertidaNoAvisa() {
        entrada(paciente("revertida@oferta.test"), consulta, "2026-11-01T10:00:00Z");
        var turno = turno();

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            cancelaciones.cancelar(turno, ActorCancelacion.USUARIO, recepcionista, null);
            estado.setRollbackOnly();
        });

        assertThat(ofertas()).isEmpty();
        assertThat(correo.intentos()).isZero();
        assertThat(jdbc.queryForObject("select estado from turno where id = ?", String.class, turno))
                .isEqualTo("PROGRAMADO");
    }

    // --- 3.5 ---

    @Test
    @DisplayName("Liberar dos veces el mismo hueco no duplica ofertas")
    void liberarDosVecesElMismoHuecoNoDuplicaOfertas() throws Exception {
        var paciente = paciente("una-tanda@oferta.test");
        var entrada = entrada(paciente, consulta, "2026-11-01T10:00:00Z");
        cancelar(turno()).andExpect(status().isOk());
        var nuevo = paciente("despues@oferta.test");
        var entradaNueva = entrada(nuevo, consulta, "2026-11-20T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(entradasConOferta()).containsExactly(entrada, entradaNueva);
        assertThat(destinatarios()).containsExactly(email(paciente), email(nuevo));
    }

    // --- 3.6 ---

    @Test
    @DisplayName("El email lleva el enlace con el token de la oferta")
    void elEmailLlevaElEnlaceConElTokenDeLaOferta() throws Exception {
        var paciente = paciente("enlace@oferta.test");
        entrada(paciente, consulta, "2026-11-01T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        var mensaje = correo.enviados().get(0);
        assertThat(mensaje.destinatario()).isEqualTo(email(paciente));
        assertThat(mensaje.asunto()).startsWith("Hueco disponible en la lista de espera");
        assertThat(mensaje.cuerpo()).contains("Fecha y hora: 04/12/2026 09:00", "Prestación: Prestación ",
                "Profesional: Luis Ficticio");
        var hash = (String) ofertas().get(0).get("token_hash");
        assertThat(new ServicioToken().coincide(token(mensaje), hash)).isTrue();
    }

    @Test
    @DisplayName("Un paciente sin email no genera oferta")
    void unPacienteSinEmailNoGeneraOferta() throws Exception {
        entrada(paciente(null), consulta, "2026-11-01T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(ofertas()).isEmpty();
        assertThat(correo.intentos()).isZero();
    }

    @Test
    @DisplayName("El aviso queda registrado asociado a la oferta")
    void elAvisoQuedaRegistradoAsociadoALaOferta() throws Exception {
        entrada(paciente("registro@oferta.test"), consulta, "2026-11-01T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        var oferta = (Long) ofertas().get(0).get("id");
        var registros = jdbc.queryForList("select * from notificacion_enviada where oferta_hueco_id = ?", oferta);
        assertThat(registros).singleElement().satisfies(fila -> {
            assertThat(fila).containsEntry("tipo", "OFERTA_HUECO").containsEntry("estado", "ENVIADA");
            assertThat(((java.sql.Timestamp) fila.get("inicio_referencia")).toInstant()).isEqualTo(INICIO_HUECO);
            assertThat(fila.get("turno_id")).isNull();
        });
    }

    @Test
    @DisplayName("Solo se guarda el hash del token")
    void soloSeGuardaElHashDelToken() throws Exception {
        entrada(paciente("hash@oferta.test"), consulta, "2026-11-01T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        var fila = ofertas().get(0);
        var claro = token(correo.enviados().get(0));
        assertThat((String) fila.get("token_hash")).matches("[0-9a-f]{64}")
                .isEqualTo(new ServicioToken().hashear(claro));
        assertThat(fila.values()).noneMatch(valor -> valor != null && valor.toString().contains(claro));
        assertThat(fila).containsEntry("estado", "ENVIADA");
        assertThat(((java.sql.Timestamp) fila.get("expira_en")).toInstant()).isEqualTo(INICIO_HUECO);
    }

    // --- 3.7 ---

    @Test
    @DisplayName("El token no aparece en los logs")
    void elTokenNoApareceEnLosLogs(CapturedOutput salida) throws Exception {
        entrada(paciente("log-a@oferta.test"), consulta, "2026-11-01T10:00:00Z");
        entrada(paciente("log-b@oferta.test"), consulta, "2026-11-02T10:00:00Z");

        cancelar(turno()).andExpect(status().isOk());

        assertThat(correo.enviados()).hasSize(2);
        for (var mensaje : correo.enviados()) {
            assertThat(salida.getAll()).doesNotContain(token(mensaje));
        }
    }
}
