package ar.edu.utn.turnos.listaespera.oferta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.turnos.listaespera.oferta.domain.EstadoOferta;
import ar.edu.utn.turnos.shared.notificacion.application.ServicioNotificaciones;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Ciclo de una oferta después de creada (C-36, RN-38): reintento del aviso con rotación de token, vencimiento al
 * inicio del hueco y búsqueda por token. El reloj fijo es {@link #AHORA}.
 */
class CicloOfertaHuecoIT extends EscenarioOfertasIT {
    static final ServicioToken TOKENS = new ServicioToken();

    @Autowired
    OfertasHueco servicio;

    @Autowired
    ServicioNotificaciones notificaciones;

    /** Cancela con el primer envío fallido y deja el reintento vencido; devuelve el id de la oferta. */
    Long ofertaConAvisoFallido() throws Exception {
        entrada(paciente("reintento@oferta.test"), consulta, "2026-11-01T10:00:00Z");
        correo.fallarProximos(1);
        cancelar(turno()).andExpect(status().isOk());
        var oferta = (Long) ofertas().get(0).get("id");
        assertThat(estadoAviso(oferta)).isEqualTo("FALLIDA");
        jdbc.update("update notificacion_enviada set programada_para = ? where oferta_hueco_id = ?",
                Timestamp.from(AHORA.minus(Duration.ofMinutes(1))), oferta);
        return oferta;
    }

    String estadoAviso(Long oferta) {
        return jdbc.queryForObject("select estado from notificacion_enviada where oferta_hueco_id = ?", String.class,
                oferta);
    }

    String hash(Long oferta) {
        return jdbc.queryForObject("select token_hash from oferta_hueco where id = ?", String.class, oferta);
    }

    /** Oferta insertada directo, con el hueco que empieza en {@code inicio}; devuelve su id. */
    Long oferta(String token, Instant inicio, String estado) {
        var entrada = entrada(paciente("ciclo@oferta.test"), consulta, "2026-11-01T10:00:00Z");
        return jdbc.queryForObject("""
                insert into oferta_hueco (lista_espera_id, profesional_id, hueco_inicio, hueco_fin, sillon_sugerido_id,
                                          token_hash, expira_en, estado, creado_en)
                values (?, ?, ?, ?, ?, ?, ?, ?, now()) returning id
                """, Long.class, entrada, profesional, Timestamp.from(inicio),
                Timestamp.from(inicio.plus(Duration.ofMinutes(30))), sillon, TOKENS.hashear(token),
                Timestamp.from(inicio), estado);
    }

    String estado(Long oferta) {
        return jdbc.queryForObject("select estado from oferta_hueco where id = ?", String.class, oferta);
    }

    // --- 4.3 ---

    @Test
    @DisplayName("El reintento envía un enlace nuevo y válido")
    void elReintentoEnviaUnEnlaceNuevoYValido() throws Exception {
        var oferta = ofertaConAvisoFallido();
        var hashAnterior = hash(oferta);

        notificaciones.procesarVencidas();

        assertThat(correo.enviados()).hasSize(1);
        var tokenNuevo = token(correo.enviados().get(0));
        assertThat(hash(oferta)).isNotEqualTo(hashAnterior).isEqualTo(TOKENS.hashear(tokenNuevo));
        assertThat(estadoAviso(oferta)).isEqualTo("ENVIADA");
        assertThat(servicio.buscarVigentePorToken(tokenNuevo)).hasValueSatisfying(
                encontrada -> assertThat(encontrada.id()).isEqualTo(oferta));
        assertThat(jdbc.queryForObject("select count(*) from oferta_hueco where token_hash = ?", Integer.class,
                hashAnterior)).isZero();
    }

    @Test
    @DisplayName("No se reintenta después de empezar el hueco")
    void noSeReintentaDespuesDeEmpezarElHueco() throws Exception {
        var oferta = ofertaConAvisoFallido();
        jdbc.update("update notificacion_enviada set inicio_referencia = ? where oferta_hueco_id = ?",
                Timestamp.from(AHORA), oferta);
        var hashAnterior = hash(oferta);
        int intentos = correo.intentos();

        notificaciones.procesarVencidas();

        assertThat(correo.intentos()).isEqualTo(intentos);
        assertThat(hash(oferta)).isEqualTo(hashAnterior);
    }

    @Test
    @DisplayName("No se reintenta una oferta que ya no está enviada")
    void noSeReintentaUnaOfertaQueYaNoEstaEnviada() throws Exception {
        var oferta = ofertaConAvisoFallido();
        jdbc.update("update oferta_hueco set estado = 'VENCIDA' where id = ?", oferta);
        int intentos = correo.intentos();

        notificaciones.procesarVencidas();

        assertThat(correo.intentos()).isEqualTo(intentos);
        assertThat(correo.enviados()).isEmpty();
        notificaciones.procesarVencidas();
        assertThat(correo.intentos()).isEqualTo(intentos);
    }

    // --- 5.2 ---

    @Test
    @DisplayName("Una oferta vence al inicio del hueco")
    void unaOfertaVenceAlInicioDelHueco() {
        var oferta = oferta(TOKENS.generar().claro(), AHORA, "ENVIADA");

        servicio.vencerOfertas();

        assertThat(estado(oferta)).isEqualTo(EstadoOferta.VENCIDA.name());
    }

    @Test
    @DisplayName("Una oferta futura sigue vigente")
    void unaOfertaFuturaSigueVigente() {
        var oferta = oferta(TOKENS.generar().claro(), AHORA.plus(Duration.ofMinutes(1)), "ENVIADA");

        servicio.vencerOfertas();

        assertThat(estado(oferta)).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("El vencimiento no toca ofertas tomadas y se puede repetir")
    void elVencimientoNoTocaOfertasTomadasYSePuedeRepetir() {
        var tomada = oferta(TOKENS.generar().claro(), AHORA.minus(Duration.ofHours(1)), "TOMADA");
        var vencida = oferta(TOKENS.generar().claro(), AHORA.minus(Duration.ofHours(1)), "VENCIDA");

        servicio.vencerOfertas();
        servicio.vencerOfertas();

        assertThat(estado(tomada)).isEqualTo("TOMADA");
        assertThat(estado(vencida)).isEqualTo("VENCIDA");
    }

    // --- 5.4 ---

    @Test
    @DisplayName("Un token vigente encuentra su oferta")
    void unTokenVigenteEncuentraSuOferta() {
        var token = TOKENS.generar().claro();
        var oferta = oferta(token, AHORA.plus(Duration.ofHours(2)), "ENVIADA");

        assertThat(servicio.buscarVigentePorToken(token)).hasValueSatisfying(
                encontrada -> assertThat(encontrada.id()).isEqualTo(oferta));
    }

    @Test
    @DisplayName("Tokens inválidos dan el mismo resultado")
    void tokensInvalidosDanElMismoResultado() {
        var deVencida = TOKENS.generar().claro();
        var deTomada = TOKENS.generar().claro();
        var deExpiradaSinPlanificador = TOKENS.generar().claro();
        oferta(deVencida, AHORA.plus(Duration.ofHours(2)), "VENCIDA");
        oferta(deTomada, AHORA.plus(Duration.ofHours(2)), "TOMADA");
        oferta(deExpiradaSinPlanificador, AHORA, "ENVIADA");

        assertThat(servicio.buscarVigentePorToken(TOKENS.generar().claro())).isEmpty();
        assertThat(servicio.buscarVigentePorToken("no-es-un-token")).isEmpty();
        assertThat(servicio.buscarVigentePorToken(null)).isEmpty();
        assertThat(servicio.buscarVigentePorToken(deVencida)).isEmpty();
        assertThat(servicio.buscarVigentePorToken(deTomada)).isEmpty();
        assertThat(servicio.buscarVigentePorToken(deExpiradaSinPlanificador)).isEmpty();
    }
}
