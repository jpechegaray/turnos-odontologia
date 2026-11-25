package ar.edu.utn.turnos.reservapublica.token;

import ar.edu.utn.turnos.agenda.application.TurnosParaToken;
import ar.edu.utn.turnos.agenda.domain.EstadoTurno;
import ar.edu.utn.turnos.shared.error.EstadoError;
import ar.edu.utn.turnos.shared.error.ReglaNegocioException;
import ar.edu.utn.turnos.shared.token.ServicioToken;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioTokenTurno {
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final ServicioToken criptografia = new ServicioToken();
    private final TurnosParaToken turnos;
    private final TokenTurnoRepositorio tokens;
    private final Clock reloj;

    ServicioTokenTurno(TurnosParaToken turnos, TokenTurnoRepositorio tokens, Clock reloj) {
        this.turnos = turnos;
        this.tokens = tokens;
        this.reloj = reloj;
    }

    @Transactional
    public TokenEmitido emitir(Long turnoId) {
        var turno = turnos.bloquearParaToken(turnoId).orElseThrow(ServicioTokenTurno::emisionRechazada);
        var ahora = reloj.instant();
        if (turno.estado() == EstadoTurno.CANCELADO || !ahora.isBefore(turno.inicio())) {
            throw emisionRechazada();
        }
        var generado = criptografia.generar();
        tokens.save(new TokenTurno(turnoId, generado.hash(), turno.inicio(), ahora));
        return new TokenEmitido(generado.claro());
    }

    @Transactional
    public Long validar(String claro) {
        return validar(claro, null);
    }

    @Transactional
    public Long validar(String claro, Long turnoEsperado) {
        if (claro == null || !FORMATO.matcher(claro).matches()) {
            throw tokenNoEncontrado();
        }
        String hash = criptografia.hashear(claro);
        Long turnoId = tokens.buscarTurno(hash).orElseThrow(ServicioTokenTurno::tokenNoEncontrado);
        if (turnoEsperado != null && !turnoEsperado.equals(turnoId)) {
            throw tokenNoEncontrado();
        }
        var turno = turnos.bloquearParaToken(turnoId).orElseThrow(ServicioTokenTurno::tokenNoEncontrado);
        var vigencia = tokens.buscarVigencia(hash).orElseThrow(ServicioTokenTurno::tokenNoEncontrado);
        var ahora = reloj.instant();
        if (!criptografia.coincide(claro, vigencia.getTokenHash()) || vigencia.getRevocadoEn() != null
                || turno.estado() == EstadoTurno.CANCELADO || !ahora.isBefore(vigencia.getExpiraEn())) {
            throw tokenNoEncontrado();
        }
        tokens.registrarUso(hash, ahora);
        return turnoId;
    }

    private static ReglaNegocioException tokenNoEncontrado() {
        return new ReglaNegocioException("TOKEN_TURNO_NO_ENCONTRADO", EstadoError.NO_ENCONTRADO_404,
                "Token de turno no encontrado");
    }

    private static ReglaNegocioException emisionRechazada() {
        return new ReglaNegocioException("TOKEN_TURNO_EMISION_RECHAZADA", EstadoError.NO_PROCESABLE_422,
                "No se puede emitir un token para ese turno");
    }

    /** El secreto solo sale de la emisión; no se incluye en representaciones de texto. */
    public record TokenEmitido(String claro) {
        @Override
        public String toString() {
            return "TokenEmitido[claro=***]";
        }
    }
}
